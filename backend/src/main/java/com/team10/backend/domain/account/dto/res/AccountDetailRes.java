package com.team10.backend.domain.account.dto.res;

import com.team10.backend.domain.account.entity.Account;
import com.team10.backend.domain.account.type.AccountStatus;
import com.team10.backend.domain.account.type.AccountType;

import java.time.LocalDateTime;
import java.util.Objects;

public record AccountDetailRes(
        Long id,
        String accountNumber,
        String nickname,
        AccountType accountType,
        Long balance,
        AccountStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static AccountDetailRes from(Account account) {
        Objects.requireNonNull(account, "account는 null일 수 없습니다.");

        return new AccountDetailRes(
                account.getId(),
                account.getAccountNumber(),
                account.getNickname(),
                account.getAccountType(),
                account.getBalance(),
                account.getStatus(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}