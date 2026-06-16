package com.team10.backend.domain.exchange.dto.req;

import com.team10.backend.domain.exchange.type.CurrencyCode;
import jakarta.validation.constraints.NotNull;

public record FxWalletCreateReq(
        @NotNull(message = "통화는 필수입니다.")
        CurrencyCode currencyCode
) {
}
