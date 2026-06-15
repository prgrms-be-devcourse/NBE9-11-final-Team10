package com.team10.backend.domain.exchange.dto.req;

import com.team10.backend.domain.exchange.type.CurrencyCode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record ExchangeQuoteCreateReq(
        @NotNull(message = "출금 통화는 필수입니다.")
        CurrencyCode fromCurrency,

        @NotNull(message = "입금 통화는 필수입니다.")
        CurrencyCode toCurrency,

        @NotNull(message = "환전 금액은 필수입니다.")
        @Positive(message = "환전 금액은 0보다 커야 합니다.")
        BigDecimal fromAmount
) {
}
