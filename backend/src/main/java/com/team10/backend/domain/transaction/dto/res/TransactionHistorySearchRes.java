package com.team10.backend.domain.transaction.dto.res;

import com.team10.backend.domain.transaction.type.TransactionDirection;
import java.time.LocalDateTime;

public record TransactionHistorySearchRes(
        Long transactionHistoryId,
        String counterpartyName,
        Long amount,
        Long balanceAfter,
        LocalDateTime transactedAt,
        TransactionDirection direction
) {
}
