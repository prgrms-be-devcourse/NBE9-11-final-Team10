package com.team10.backend.domain.exAccount.dto.res;

import com.team10.backend.domain.exAccount.Type.ExAccountStatus;
import com.team10.backend.domain.exAccount.Type.ExAccountType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExAccountRes(
        Long id,
        String organization,
        String accountNoMasked,
        String accountName,
        String accountAlias,
        ExAccountType assetType,
        BigDecimal balance,
        BigDecimal withdrawableAmount,
        LocalDate openedAt,
        LocalDate maturityAt,
        LocalDate lastTransactionAt,
        ExAccountStatus status
) {
}
