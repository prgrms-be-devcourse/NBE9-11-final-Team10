package com.team10.backend.domain.codef.exAccount.application.dto.internal;

import com.team10.backend.domain.exAccount.domain.type.ExAccountType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CodefExAccountSnapshot(
        String organization,
        String accountNumber,
        String accountName,
        String accountAlias,
        ExAccountType assetType,
        BigDecimal balance,
        BigDecimal withdrawableAmount,
        LocalDate openedAt,
        LocalDate maturityAt,
        LocalDate lastTransactionAt
) {
}
