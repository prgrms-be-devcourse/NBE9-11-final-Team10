package com.team10.backend.domain.account.dto.res;

import com.team10.backend.domain.account.type.AccountStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AccountSummaryRes {

    private Long id;
    private String accountNumber;
    private String nickname;
    private Long balance;
    private AccountStatus status;
    private LocalDateTime createdAt;
}