package com.team10.backend.domain.exchange.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ExchangeOrderCreateReq(
        @NotNull(message = "환전 견적 ID는 필수입니다.")
        Long exchangeQuoteId,

        @NotNull(message = "원화 계좌 ID는 필수입니다.")
        Long krwAccountId,

        @NotNull(message = "외화 지갑 ID는 필수입니다.")
        Long fxWalletId,

        @NotBlank(message = "멱등성 키는 필수입니다.")
        String idempotencyKey
) {
}
