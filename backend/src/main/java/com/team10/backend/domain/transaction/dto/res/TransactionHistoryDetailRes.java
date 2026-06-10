package com.team10.backend.domain.transaction.dto.res;

import com.team10.backend.domain.transaction.entity.TransactionHistory;
import com.team10.backend.domain.transaction.type.TransactionDirection;
import com.team10.backend.domain.transaction.type.TransactionType;
import java.time.LocalDateTime;

public record TransactionHistoryDetailRes(
        Long transactionHistoryId,
        TransactionType type,
        TransactionDirection direction,
        Long amount,
        Long balanceAfter,
        String counterpartyName,
        String memo,
        LocalDateTime transactedAt
) {

    public static TransactionHistoryDetailRes from(TransactionHistory transactionHistory) {
        return new TransactionHistoryDetailRes(
                transactionHistory.getId(),
                transactionHistory.getType(),
                transactionHistory.getDirection(),
                transactionHistory.getAmount(),
                transactionHistory.getBalanceAfter(),
                transactionHistory.getCounterpartyName(),
                transactionHistory.getMemo(),
                transactionHistory.getTransactedAt()
        );
    }
}
