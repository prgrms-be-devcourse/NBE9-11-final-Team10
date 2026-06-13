package com.team10.backend.domain.exchange.dto.res;

import com.team10.backend.domain.exchange.entity.ExchangeQuote;
import com.team10.backend.domain.exchange.type.CurrencyCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExchangeQuoteRes(
        Long exchangeQuoteId,
        CurrencyCode fromCurrency,
        CurrencyCode toCurrency,
        BigDecimal fromAmount,
        BigDecimal rate,
        BigDecimal feeRate,
        BigDecimal fee,
        BigDecimal expectedToAmount,
        LocalDateTime expiredAt,
        LocalDateTime createdAt
) {

    public static ExchangeQuoteRes from(ExchangeQuote exchangeQuote) {
        return new ExchangeQuoteRes(
                exchangeQuote.getId(),
                exchangeQuote.getFromCurrency().getCurrencyCode(),
                exchangeQuote.getToCurrency().getCurrencyCode(),
                exchangeQuote.getFromAmount(),
                exchangeQuote.getRate(),
                exchangeQuote.getFeeRate(),
                exchangeQuote.getFee(),
                exchangeQuote.getExpectedToAmount(),
                exchangeQuote.getExpiredAt(),
                exchangeQuote.getCreatedAt()
        );
    }
}
