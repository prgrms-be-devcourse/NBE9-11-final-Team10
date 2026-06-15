package com.team10.backend.domain.account.dto.res;

import com.team10.backend.domain.account.entity.Account;
import com.team10.backend.domain.account.type.AccountStatus;

public record AccountSummaryRes(
        Long id,
        String accountNumber,
        String nickname,
        Long balance,
        AccountStatus status
) {

    public static AccountSummaryRes from(Account account) {
        return new AccountSummaryRes(
                account.getId(),
                account.getAccountNumber(),
                account.getNickname(),
                account.getBalance(),
                account.getStatus()
        );
    }
}