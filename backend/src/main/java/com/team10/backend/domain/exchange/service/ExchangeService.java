package com.team10.backend.domain.exchange.service;

import com.team10.backend.domain.account.entity.Account;
import com.team10.backend.domain.account.exception.AccountErrorCode;
import com.team10.backend.domain.account.repository.AccountRepository;
import com.team10.backend.domain.exchange.calculator.ExchangeCalculator;
import com.team10.backend.domain.exchange.calculator.QuoteCalculation;
import com.team10.backend.domain.exchange.dto.res.ExchangeOrderRes;
import com.team10.backend.domain.exchange.dto.res.ExchangeQuoteRes;
import com.team10.backend.domain.exchange.entity.*;
import com.team10.backend.domain.exchange.exception.ExchangeErrorCode;
import com.team10.backend.domain.exchange.repository.*;
import com.team10.backend.domain.exchange.type.CurrencyCode;
import com.team10.backend.domain.exchange.type.ExchangeDirection;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.domain.user.repository.UserRepository;
import com.team10.backend.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ExchangeService {

    private final ExchangeQuoteRepository exchangeQuoteRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final UserRepository userRepository;
    private final CurrencyRepository currencyRepository;
    private final ExchangeCalculator exchangeCalculator;
    private final AccountRepository accountRepository;
    private final FxWalletRepository fxWalletRepository;
    private final ExchangeOrderRepository exchangeOrderRepository;


    @Transactional
    public ExchangeQuoteRes createQuote(
            Long userId,
            CurrencyCode fromCurrencyCode,
            CurrencyCode toCurrencyCode,
            BigDecimal fromAmount
    ) {
        // 사용자 조회 및 검증
        User user = findUser(userId);
        // 동일 통화 검증
        validateDifferentCurrencies(fromCurrencyCode, toCurrencyCode);
        validateKrwPair(fromCurrencyCode, toCurrencyCode);

        // 통화 조회
        Currency fromCurrency = findCurrency(fromCurrencyCode);
        Currency toCurrency = findCurrency(toCurrencyCode);

        CurrencyCode foreignCurrencyCode = resolveForeignCurrency(fromCurrencyCode, toCurrencyCode);

        // 최신 환율 조회
        ExchangeRate rate = exchangeRateRepository.findByCurrencyCurrencyCode(foreignCurrencyCode)
                .orElseThrow(() -> new BusinessException(ExchangeErrorCode.EXCHANGE_RATE_NOT_FOUND));

        // 계산기 호출
        QuoteCalculation calculation = exchangeCalculator.calculate(
                fromCurrency.getCurrencyCode(),
                toCurrency.getCurrencyCode(),
                fromAmount,
                rate.getBasePrice(),
                rate.getCurrencyUnit(),
                fromCurrency.getDecimalPlaces(),
                toCurrency.getDecimalPlaces()
        );

        // ExchangeQuote 생성/저장
        ExchangeQuote quote = ExchangeQuote.create(
                user,
                fromCurrency,
                toCurrency,
                fromAmount,
                calculation.rate(),
                calculation.feeRate(),
                calculation.fee(),
                calculation.expectedToAmount(),
                LocalDateTime.now().plusMinutes(5)
        );

        return ExchangeQuoteRes.from(exchangeQuoteRepository.save(quote));
    }

    private void validateDifferentCurrencies(CurrencyCode fromCurrencyCode, CurrencyCode toCurrencyCode) {
        if (fromCurrencyCode == toCurrencyCode) {
            throw new BusinessException(ExchangeErrorCode.SAME_CURRENCY_EXCHANGE_NOT_ALLOWED);
        }
    }

    private void validateKrwPair(CurrencyCode fromCurrencyCode, CurrencyCode toCurrencyCode) {
        boolean fromIsKRW = fromCurrencyCode == CurrencyCode.KRW;
        boolean toIsKRW = toCurrencyCode == CurrencyCode.KRW;

        // 둘 다 KRW or !KRW => 유효하지 않은 환전 방향
        if (fromIsKRW == toIsKRW) {
            throw new BusinessException(ExchangeErrorCode.INVALID_EXCHANGE_DIRECTION);
        }
    }

    private Currency findCurrency(CurrencyCode currencyCode) {
        return currencyRepository.findByCurrencyCode(currencyCode)
                .orElseThrow(() -> new BusinessException(ExchangeErrorCode.CURRENCY_NOT_FOUND));
    }

    private CurrencyCode resolveForeignCurrency(CurrencyCode fromCurrencyCode, CurrencyCode toCurrencyCode) {
        boolean fromIsKRW = fromCurrencyCode == CurrencyCode.KRW;
        return fromIsKRW ? toCurrencyCode : fromCurrencyCode; // 외화만 반환
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExchangeErrorCode.USER_NOT_FOUND));
    }

    // TODO: @Idempotency 적용
    @Transactional
    public ExchangeOrderRes createExchangeOrder(
            Long userId,
            String idempotencyKey,
            Long exchangeQuoteId,
            Long krwAccountId,
            Long fxWalletId
    ) {
        // 사용자 확인
        User user = findUser(userId);

        // 견적 확인
        ExchangeQuote quote = getOwnedUsableQuote(userId, exchangeQuoteId);

        // 방향 결정
        ExchangeDirection direction = resolveDirection(quote);

        // 계좌/지갑 락 부여 및 검증
        ExchangeResources exchangeResources = lockAndValidateResources(userId, krwAccountId, fxWalletId, quote);

        Account krwAccount = exchangeResources.krwAccount;
        FxWallet fxWallet = exchangeResources.fxWallet;

        // 잔액 반영
        applyExchange(direction, quote, krwAccount, fxWallet);

        // 완료한 주문 저장
        ExchangeOrder order = saveCompletedOrder(user, quote, krwAccount, fxWallet, direction);

        return ExchangeOrderRes.from(order);
    }

    private ExchangeDirection resolveDirection(ExchangeQuote quote) {
        return quote.getFromCurrency().getCurrencyCode() == CurrencyCode.KRW
                ? ExchangeDirection.KRW_TO_FOREIGN
                : ExchangeDirection.FOREIGN_TO_KRW;
    }

    private ExchangeQuote getOwnedUsableQuote(Long userId, Long exchangeQuoteId) {
        // 견적 존재 검증
        ExchangeQuote quote = exchangeQuoteRepository.findById(exchangeQuoteId)
                .orElseThrow(() -> new BusinessException(ExchangeErrorCode.EXCHANGE_QUOTE_NOT_FOUND));

        // 견적 소유자 검증
        if (!userId.equals(quote.getUser().getId())) {
            throw new BusinessException(ExchangeErrorCode.EXCHANGE_QUOTE_ACCESS_DENIED);
        }

        // 견적 재사용 검증
        if (exchangeOrderRepository.existsByExchangeQuote_Id(exchangeQuoteId)) {
            throw new BusinessException(ExchangeErrorCode.EXCHANGE_QUOTE_ALREADY_USED);
        }

        // 이미 만료된 견적인지 검증
        if (quote.isExpired(LocalDateTime.now())) {
            throw new BusinessException(ExchangeErrorCode.EXCHANGE_QUOTE_EXPIRED);
        }

        return quote;
    }

    private ExchangeResources lockAndValidateResources(
            Long userId,
            Long krwAccountId,
            Long fxWalletId,
            ExchangeQuote quote
    ) {
        // 원화 계좌 -> 외화 지갑 순서로 락 획득
        Account krwAccount = accountRepository.findByIdAndUserIdForUpdate(krwAccountId, userId)
                .orElseThrow(() -> new BusinessException(AccountErrorCode.ACCOUNT_NOT_FOUND));

        FxWallet fxWallet = fxWalletRepository.findByIdAndUserIdForUpdate(fxWalletId, userId)
                .orElseThrow(() -> new BusinessException(ExchangeErrorCode.FX_WALLET_NOT_FOUND));

        validateExchangeResources(krwAccount, fxWallet, quote);

        return new ExchangeResources(krwAccount, fxWallet);
    }

    private void validateExchangeResources(Account krwAccount, FxWallet fxWallet, ExchangeQuote quote) {
        // 원화 지갑 & 외화 지갑 상태 확인
        if (!krwAccount.isActive()) {
            throw new BusinessException(AccountErrorCode.ACCOUNT_NOT_ACTIVE);
        }
        if (!fxWallet.isActive()) {
            throw new BusinessException(ExchangeErrorCode.FX_WALLET_NOT_ACTIVE);
        }
        // 지갑 통화와 견적 외화 통화가 일치하는지 검증
        if (fxWallet.getCurrency().getCurrencyCode() != quote.getFxCurrency().getCurrencyCode()) {
            throw new BusinessException(ExchangeErrorCode.FX_WALLET_CURRENCY_MISMATCH);
        }
    }

    private void applyExchange(
            ExchangeDirection direction,
            ExchangeQuote quote,
            Account krwAccount,
            FxWallet fxWallet
    ) {
        // 원화 -> 외화
        if (direction == ExchangeDirection.KRW_TO_FOREIGN) {
            krwAccount.withdraw(toKrwLong(quote.getFromAmount()));  // 원화 계좌 차감
            fxWallet.deposit(quote.getExpectedToAmount());          // 외화 지갑 입금
            return;
        }

        // 외화 -> 원화
        fxWallet.withdraw(quote.getFromAmount());                   // 외화 지갑 차감
        krwAccount.deposit(toKrwLong(quote.getExpectedToAmount())); // 원화 계좌 입금
    }

    private ExchangeOrder saveCompletedOrder(
            User user,
            ExchangeQuote quote,
            Account krwAccount,
            FxWallet fxWallet,
            ExchangeDirection direction
    ) {
        return exchangeOrderRepository.save(ExchangeOrder.createCompleted(
                user,
                quote,
                krwAccount,
                fxWallet,
                direction,
                LocalDateTime.now()
        ));
    }

    private Long toKrwLong(BigDecimal amount) {
        try {
            return amount.longValueExact();
        } catch (ArithmeticException e) {
            throw new BusinessException(ExchangeErrorCode.INVALID_EXCHANGE_AMOUNT);
        }
    }

    private record ExchangeResources(
            Account krwAccount,
            FxWallet fxWallet
    ) {
    }
}
