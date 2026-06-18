package com.team10.backend.domain.exAccount.dto.res;

import java.math.BigDecimal;

public record ExAccountSummaryRes(
        BigDecimal totalAssetAmount,
        BigDecimal totalDebtAmount,
        BigDecimal netAssetAmount,
        BigDecimal monthlyIncomeAmount,
        BigDecimal monthlyExpenseAmount,
        long accountCount,
        long loanCount
) {

}
