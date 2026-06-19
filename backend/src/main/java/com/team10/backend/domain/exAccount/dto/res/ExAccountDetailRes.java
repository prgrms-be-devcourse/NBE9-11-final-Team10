package com.team10.backend.domain.exAccount.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "외부 계좌 상세 및 거래내역 응답")
public record ExAccountDetailRes(
        @Schema(description = "외부 계좌 상세 정보")
        ExAccountRes account,

        @Schema(description = "외부 계좌 거래내역")
        List<ExAccountTransactionRes> transactions
) {
    public static ExAccountDetailRes of(ExAccountRes account, List<ExAccountTransactionRes> transactions) {
        return new ExAccountDetailRes(account, transactions);
    }
}
