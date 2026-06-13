package com.team10.backend.domain.exchange.service;

import com.team10.backend.domain.exchange.client.KoreaEximExchangeRateClient;
import com.team10.backend.domain.exchange.client.KoreaEximExchangeRateRes;
import com.team10.backend.domain.exchange.dto.res.ExchangeRateRes;
import com.team10.backend.domain.exchange.entity.Currency;
import com.team10.backend.domain.exchange.entity.ExchangeRate;
import com.team10.backend.domain.exchange.exception.ExchangeErrorCode;
import com.team10.backend.domain.exchange.repository.CurrencyRepository;
import com.team10.backend.domain.exchange.repository.ExchangeRateRepository;
import com.team10.backend.domain.exchange.type.CurrencyCode;
import com.team10.backend.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 오늘 날짜 API 호출
 -> 데이터 없으면 최대 7일 전까지 fallback
 -> result == 1인 응답만 사용
 -> JPY(100), IDR(100)은 코드에서 (100) 제거
 -> enum에 없는 통화는 저장하지 않고 스킵
 -> 같은 통화 + 같은 rateAt 날짜는 중복 저장하지 않음
 */

@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private static final int MAX_FALLBACK_DAYS = 7;
    private static final Set<CurrencyCode> SUPPORTED_CURRENCIES = Arrays.stream(CurrencyCode.values())
            .collect(Collectors.toUnmodifiableSet()); // 현재 서비스에서 제공하는 외화

    private final KoreaEximExchangeRateClient koreaEximExchangeRateClient;
    private final CurrencyRepository currencyRepository;
    private final ExchangeRateRepository exchangeRateRepository;

    // 일일 1번 동기화
    @Transactional
    public List<ExchangeRateRes> syncTodayRates() {
        ExchangeRateFetchResult fetchResult = fetchLatestAvailableRates(LocalDate.now());
        LocalDateTime rateAt = fetchResult.rateDate().atStartOfDay();

        fetchResult.rates().stream()
                .filter(this::isSuccess)
                .forEach(rate -> saveIfSupported(rate, rateAt));

        return exchangeRateRepository.findAllByRateAtOrderByCurrencyCodeAsc(rateAt).stream()
                .map(ExchangeRateRes::from)
                .toList();
    }

    // 최신 환율 리스트 조회
    @Transactional(readOnly = true)
    public List<ExchangeRateRes> getLatestRates() {
        LocalDateTime latestRateAt = exchangeRateRepository.findLatestRateAt()
                .orElseThrow(() -> new BusinessException(ExchangeErrorCode.EXCHANGE_RATE_NOT_FOUND));

        return exchangeRateRepository.findAllByRateAtOrderByCurrencyCodeAsc(latestRateAt).stream()
                .map(ExchangeRateRes::from)
                .toList();
    }

    // 특정 통화의 최신 환율 조회
    @Transactional(readOnly = true)
    public ExchangeRateRes getLatestRate(CurrencyCode currencyCode) {
        return exchangeRateRepository.findLatestByCurrencyCode(currencyCode)
                .map(ExchangeRateRes::from)
                .orElseThrow(() -> new BusinessException(ExchangeErrorCode.EXCHANGE_RATE_NOT_FOUND));
    }

    private ExchangeRateFetchResult fetchLatestAvailableRates(LocalDate baseDate) {
        // 당일부터 최대 7일전까지 확인
        for (int daysAgo = 0; daysAgo <= MAX_FALLBACK_DAYS; daysAgo++) {
            LocalDate searchDate = baseDate.minusDays(daysAgo); // 찾을 날짜
            List<KoreaEximExchangeRateRes> rates = fetchRates(searchDate);

            if (hasAvailableSupportedRate(rates)) {
                return new ExchangeRateFetchResult(searchDate, rates);
            }
        }

        throw new BusinessException(ExchangeErrorCode.EXCHANGE_RATE_NOT_FOUND);
    }

    private List<KoreaEximExchangeRateRes> fetchRates(LocalDate date) {
        try {
            List<KoreaEximExchangeRateRes> rates = koreaEximExchangeRateClient.fetch(date);
            return rates == null ? List.of() : rates;
        } catch (RuntimeException exception) {
            throw new BusinessException(ExchangeErrorCode.EXCHANGE_RATE_SYNC_FAILED, exception);
        }
    }

    private boolean hasAvailableSupportedRate(List<KoreaEximExchangeRateRes> rates) {
        return rates.stream()
                .filter(this::isSuccess)
                .map(rate -> toCurrencyCode(rate.curUnit()))
                .flatMap(Optional::stream)
                .anyMatch(SUPPORTED_CURRENCIES::contains);
    }

    private void saveIfSupported(KoreaEximExchangeRateRes response, LocalDateTime rateAt) {
        toCurrencyCode(response.curUnit())
                .filter(SUPPORTED_CURRENCIES::contains)
                .ifPresent(currencyCode -> saveIfAbsent(currencyCode, response, rateAt));
    }

    private void saveIfAbsent(CurrencyCode currencyCode, KoreaEximExchangeRateRes response, LocalDateTime rateAt) {
        if (exchangeRateRepository.findByCurrencyCodeAndRateAt(currencyCode, rateAt).isPresent()) {
            return;
        }

        Currency currency = currencyRepository.findByCurrencyCode(currencyCode)
                .orElseGet(() -> currencyRepository.save(Currency.create(
                        currencyCode,
                        response.curName(),
                        response.curName(),
                        decimalPlaces(currencyCode)
                )));

        ExchangeRate exchangeRate = ExchangeRate.create(
                currency,
                parseRate(response.ttb()),
                parseRate(response.tts()),
                parseRate(response.dealBasR()),
                rateAt
        );

        exchangeRateRepository.save(exchangeRate);
    }

    private boolean isSuccess(KoreaEximExchangeRateRes response) {
        return response != null && Integer.valueOf(1).equals(response.result()); // 응답이 not null & "result": 1 인 경우
    }

    private Optional<CurrencyCode> toCurrencyCode(String curUnit) {
        if (curUnit == null || curUnit.isBlank()) {
            return Optional.empty(); // 외화 종류가 null or "" 인 경우
        }

        String normalizedCode = curUnit.replace("(100)", ""); // "cur_unit": "JPY(100)"처럼 100원 단위인 경우

        try {
            return Optional.of(CurrencyCode.valueOf(normalizedCode)); // Optional객체화 시켜서 반환
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private BigDecimal parseRate(String value) {
        return new BigDecimal(value.replace(",", ""));
    }

    private Integer decimalPlaces(CurrencyCode currencyCode) {
        return switch (currencyCode) {
            case KRW, JPY -> 0;
            default -> 2;
        };
    }

    private record ExchangeRateFetchResult(
            LocalDate rateDate,
            List<KoreaEximExchangeRateRes> rates
    ) {
    }
}
