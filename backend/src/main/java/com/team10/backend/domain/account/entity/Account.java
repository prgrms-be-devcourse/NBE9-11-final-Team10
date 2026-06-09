package com.team10.backend.domain.account.entity;

import com.team10.backend.domain.account.type.AccountStatus;
import com.team10.backend.domain.account.type.AccountType;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "accounts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 30)
    private String accountNumber;

    @Column(length = 50)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType accountType;

    @Column(nullable = false)
    private Long balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    public static Account create(
            User user,
            String accountNumber,
            String nickname,
            AccountType accountType
    ) {
        Account account = new Account();
        account.user = user;
        account.accountNumber = accountNumber;
        account.nickname = nickname;
        account.accountType = accountType;
        account.balance = 0L;
        account.status = AccountStatus.ACTIVE;
        return account;
    }
}
