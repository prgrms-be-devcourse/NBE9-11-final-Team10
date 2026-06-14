package com.team10.backend.domain.exchange.service;

import com.team10.backend.domain.exchange.client.UpbitExchangeRateClient;
import com.team10.backend.domain.exchange.client.UpbitExchangeRateRes;
import com.team10.backend.domain.exchange.dto.res.ExchangeRateRes;
import com.team10.backend.domain.exchange.entity.Currency;
import com.team10.backend.domain.exchange.entity.ExchangeRate;
import com.team10.backend.domain.exchange.exception.ExchangeErrorCode;
import com.team10.backend.domain.exchange.repository.CurrencyRepository;
import com.team10.backend.domain.exchange.repository.ExchangeRateCacheRepository;
import com.team10.backend.domain.exchange.repository.ExchangeRateRepository;
import com.team10.backend.domain.exchange.type.CurrencyCode;
import com.team10.backend.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 스케줄러로 30초마다 최신 환율만 유지
 * 존재하면 업데이트, 없으면 새로 추가
 * */
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private static final Set<CurrencyCode> SUPPORTED_CURRENCIES = Arrays.stream(CurrencyCode.values())
            .collect(Collectors.toUnmodifiableSet()); // 현재 서비스에서 제공하는 외화

    private final UpbitExchangeRateClient upbitExchangeRateClient;
    private final CurrencyRepository currencyRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final ExchangeRateCacheRepository exchangeRateCacheRepository;

    // 실시간 환율 동기화
    @Transactional
    public List<ExchangeRateRes> syncCurrentRates() {
        List<CurrencyCode> targetCurrencies = getSupportedForeignCurrencies();
        List<UpbitExchangeRateRes> responses = fetchRates(targetCurrencies);

        if (responses.isEmpty()) {
            throw new BusinessException(ExchangeErrorCode.EXCHANGE_RATE_SYNC_FAILED);
        }

        responses.forEach(this::saveIfSupported); // 지원하는 통화면 동기화

        List<ExchangeRateRes> latestRates = getLatestRatesFromDb(); // DB에서 조회

        // 익명클래스 & 콜백메서드 afterCommit에서 Redis 저장 실행 -> DB 커밋 성공시에만 Redis에도 저장됨
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                exchangeRateCacheRepository.saveAll(latestRates); // Redis에 저장
            }
        });

        return getLatestRates();
    }

    private List<CurrencyCode> getSupportedForeignCurrencies() {
        return new ArrayList<>(SUPPORTED_CURRENCIES);
    }

    private List<UpbitExchangeRateRes> fetchRates(List<CurrencyCode> currencyCodes) {
        try {
            List<UpbitExchangeRateRes> rates = upbitExchangeRateClient.fetch(currencyCodes);
            return rates == null ? List.of() : rates;
        } catch (RuntimeException exception) {
            throw new BusinessException(ExchangeErrorCode.EXCHANGE_RATE_SYNC_FAILED, exception);
        }
    }

    // 최신 환율 리스트 조회(Redis에서 우선 조회)
    @Transactional(readOnly = true)
    public List<ExchangeRateRes> getLatestRates() {
        List<ExchangeRateRes> cachedRates = exchangeRateCacheRepository.findAll();

        if (!cachedRates.isEmpty()) {
            return cachedRates;
        }

        // 캐시된 값이 없는 경우
        List<ExchangeRateRes> rates = getLatestRatesFromDb();
        exchangeRateCacheRepository.saveAll(rates);
        return rates;
    }

    // 최신 환율 리스트 조회(DB에서 조회)
    @Transactional(readOnly = true)
    public List<ExchangeRateRes> getLatestRatesFromDb() {
        List<ExchangeRate> exchangeRates = exchangeRateRepository.findAllByOrderByCurrencyCurrencyCodeAsc();

        return exchangeRates.stream()
                .map(ExchangeRateRes::from)
                .toList();
    }

    // 특정 통화의 최신 환율 조회(Redis에서 우선 조회)
    @Transactional(readOnly = true)
    public ExchangeRateRes getLatestRate(CurrencyCode currencyCode) {
        return exchangeRateCacheRepository.findByCurrency(currencyCode)
                .orElseGet(() -> getLatestRateFromDb(currencyCode)); // 있으면 get, 없으면 DB에서 조회
    }

    // 특정 통화의 최신 환율 조회(DB에서 조회)
    @Transactional(readOnly = true)
    public ExchangeRateRes getLatestRateFromDb(CurrencyCode currencyCode) {
        return exchangeRateRepository.findByCurrencyCurrencyCode(currencyCode)
                .map(ExchangeRateRes::from)
                .orElseThrow(() -> new BusinessException(ExchangeErrorCode.EXCHANGE_RATE_NOT_FOUND));
    }

/*
    1. response.currencyCode()를 CurrencyCode enum으로 변환
    2. enum에 없으면 스킵
    3. Currency 조회, 없으면 생성
    4. rateAt = LocalDateTime.of(response.date(), response.time())
    5. currencyCode 기준 없으면 생성, 있으면 업데이트
    6. ExchangeRate.create(currency, basePrice, currencyUnit, rateAt) or .update(basePrice, currencyUnit, rateAt)
    7. 저장
 */
    private void saveIfSupported(UpbitExchangeRateRes response) {
        LocalDateTime rateAt = LocalDateTime.of(response.date(), response.time()); // 날짜 + 시간 => rateAt 생성

        toCurrencyCode(response.currencyCode())
                .filter(SUPPORTED_CURRENCIES::contains) // 현재 지원하는 통화만 필터링
                .ifPresent(code -> upsertRate(code, response, rateAt));
    }

    private void upsertRate(CurrencyCode currencyCode, UpbitExchangeRateRes response, LocalDateTime rateAt) {

        Currency currency = currencyRepository.findByCurrencyCode(currencyCode)
                .orElseGet(() -> currencyRepository.save(Currency.create(
                        currencyCode,
                        response.currencyName(),
                        response.country(),
                        decimalPlaces(currencyCode)
                )));

        exchangeRateRepository.findByCurrencyCurrencyCode(currencyCode)
                .ifPresentOrElse(
                        // 있으면 업데이트
                        exchangeRate -> exchangeRate.update(
                                response.basePrice(),
                                response.currencyUnit(),
                                rateAt
                        ),
                        // 없으면 새로 생성
                        () -> exchangeRateRepository.save(
                                ExchangeRate.create(
                                        currency,
                                        response.basePrice(),
                                        response.currencyUnit(),
                                        rateAt
                                )
                        )
                );
    }

    private Optional<CurrencyCode> toCurrencyCode(String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            return Optional.empty(); // 외화 종류가 null or "" 인 경우
        }

        try {
            return Optional.of(CurrencyCode.valueOf(currencyCode)); // Optional객체화 시켜서 반환
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private Integer decimalPlaces(CurrencyCode currencyCode) {
        return switch (currencyCode) {
            case JPY -> 0;
            default -> 2;
        };
    }

}
