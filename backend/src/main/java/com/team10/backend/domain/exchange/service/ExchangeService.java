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
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExchangeErrorCode.USER_NOT_FOUND));
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

    @Transactional
    public ExchangeOrderRes createExchangeOrder(
            Long userId,
            String idempotencyKey,
            Long exchangeQuoteId,
            Long krwAccountId,
            Long fxWalletId
    ) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExchangeErrorCode.USER_NOT_FOUND));

        // 견적 존재 검증
        ExchangeQuote exchangeQuote = exchangeQuoteRepository.findById(exchangeQuoteId)
                .orElseThrow(() -> new BusinessException(ExchangeErrorCode.EXCHANGE_QUOTE_NOT_FOUND));

        // 견적 소유자 검증
        if (!userId.equals(exchangeQuote.getUser().getId())) {
            throw new BusinessException(ExchangeErrorCode.EXCHANGE_QUOTE_ACCESS_DENIED);
        }

        // 견적 재사용 검증
        if (exchangeOrderRepository.existsByExchangeQuote_Id(exchangeQuoteId)) {
            throw new BusinessException(ExchangeErrorCode.EXCHANGE_QUOTE_ALREADY_USED);
        }

        // 이미 만료된 견적인지 검증
        if (exchangeQuote.isExpired(LocalDateTime.now())) {
            throw new BusinessException(ExchangeErrorCode.EXCHANGE_QUOTE_EXPIRED);
        }

//        원화 → 외화 / 외화 → 원화 방향 판단
        ExchangeDirection exchangeDirection =
                exchangeQuote.getFromCurrency().getCurrencyCode() == CurrencyCode.KRW
                        ? ExchangeDirection.KRW_TO_FOREIGN : ExchangeDirection.FOREIGN_TO_KRW;

        // 원화 계좌 -> 외화 지갑 순서로 락 획득
        Account krwAccount = accountRepository.findByIdAndUserIdForUpdate(krwAccountId, userId)
                .orElseThrow(() -> new BusinessException(AccountErrorCode.ACCOUNT_NOT_FOUND));

        FxWallet fxWallet = fxWalletRepository.findByIdAndUserIdForUpdate(fxWalletId, userId)
                .orElseThrow(() -> new BusinessException(ExchangeErrorCode.FX_WALLET_NOT_FOUND));

        // 원화 지갑 & 외화 지갑 상태 확인
        if (!krwAccount.isActive()) {
            throw new BusinessException(AccountErrorCode.ACCOUNT_NOT_ACTIVE);
        }
        if (!fxWallet.isActive()) {
            throw new BusinessException(ExchangeErrorCode.FX_WALLET_NOT_ACTIVE);
        }

        // 지갑 통화와 견적 외화 통화가 일치하는지 검증
        if (fxWallet.getCurrency().getCurrencyCode() != exchangeQuote.getFxCurrency().getCurrencyCode()) {
            throw new BusinessException(ExchangeErrorCode.EXCHANGE_PROCESSING_FAILED);
        }

        // 원화 -> 외화
        if (exchangeDirection == ExchangeDirection.KRW_TO_FOREIGN) {
            // 원화 계좌 차감
            krwAccount.withdraw(exchangeQuote.getFromAmount().longValue());

            // 외화 지갑 입금
            fxWallet.deposit(exchangeQuote.getExpectedToAmount());

        } else { // 외화 -> 원화
            // 원화 계좌 입금
            krwAccount.deposit(exchangeQuote.getExpectedToAmount().longValue());
            // 외화 지갑 차감
            fxWallet.withdraw(exchangeQuote.getFromAmount());
        }

        LocalDateTime completedAt = LocalDateTime.now();

        ExchangeOrder exchangeOrder = exchangeOrderRepository.save(
                ExchangeOrder.createCompleted(
                        user,
                        exchangeQuote,
                        krwAccount,
                        fxWallet,
                        exchangeDirection,
                        completedAt
                )
        );

        return ExchangeOrderRes.from(exchangeOrder);

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
}
