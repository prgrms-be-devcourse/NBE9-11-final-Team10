package com.team10.backend.domain.transfer.dto.req;

import jakarta.validation.constraints.*;

public record TransferReq(
        @NotNull
        Long senderAccountId,

        @NotBlank
        @Size(max = 30)
        @Pattern(regexp = "^[0-9-]+$") // 숫자(0~9)와 하이픈(-)으로만 이루어진 문자열
        String receiverAccountNumber,

        @NotNull
        @Positive
        Long amount,

        @Size(max = 100)
        String memo
) {
}
