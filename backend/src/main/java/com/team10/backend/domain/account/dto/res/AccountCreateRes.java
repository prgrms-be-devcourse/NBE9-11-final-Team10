package com.team10.backend.domain.account.dto.res;

import com.team10.backend.domain.account.entity.Account;
import com.team10.backend.domain.account.type.AccountStatus;
import com.team10.backend.domain.account.type.AccountType;

import java.time.LocalDateTime;
import java.util.Objects;

public record AccountCreateRes(
        Long id,
        String accountNumber,
        String nickname,
        AccountType accountType,
        Long balance,
        AccountStatus status,
        LocalDateTime createdAt
) {

    public static AccountCreateRes from(Account account) {
        Objects.requireNonNull(account, "account는 null일 수 없습니다.");

        return new AccountCreateRes(
                account.getId(),
                account.getAccountNumber(),
                account.getNickname(),
                account.getAccountType(),
                account.getBalance(),
                account.getStatus(),
                account.getCreatedAt()
        );
    }
}