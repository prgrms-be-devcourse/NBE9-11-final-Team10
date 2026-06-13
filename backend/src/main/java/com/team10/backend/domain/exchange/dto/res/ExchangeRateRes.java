package com.team10.backend.domain.exchange.dto.res;

import com.team10.backend.domain.exchange.entity.ExchangeRate;
import com.team10.backend.domain.exchange.type.CurrencyCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExchangeRateRes(
        Long exchangeRateId,
        CurrencyCode currency,
        BigDecimal ttb,
        BigDecimal tts,
        BigDecimal dealBasR,
        LocalDateTime rateAt
) {

    public static ExchangeRateRes from(ExchangeRate exchangeRate) {
        return new ExchangeRateRes(
                exchangeRate.getId(),
                exchangeRate.getCurrency().getCurrencyCode(),
                exchangeRate.getTtb(),
                exchangeRate.getTts(),
                exchangeRate.getDealBasR(),
                exchangeRate.getRateAt()
        );
    }
}
