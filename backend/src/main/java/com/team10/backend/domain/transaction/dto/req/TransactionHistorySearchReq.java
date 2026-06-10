package com.team10.backend.domain.transaction.dto.req;

import com.team10.backend.domain.transaction.type.TransactionDirection;
import com.team10.backend.global.validation.annotation.ValidDateRange;
import com.team10.backend.global.validation.annotation.ValidNumberRange;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@ValidDateRange(start = "startDate", end = "endDate")
@ValidNumberRange(min = "minAmount", max = "maxAmount")
public record TransactionHistorySearchReq(
        // 기간 범위
        LocalDate startDate,
        LocalDate endDate,

        // 입 , 출금
        TransactionDirection direction,

        // 거래액 범위
        @PositiveOrZero Long minAmount,
        @PositiveOrZero Long maxAmount,

        // 거래 상대
        @Size(max = 30) String counterpartyName
) {
}
