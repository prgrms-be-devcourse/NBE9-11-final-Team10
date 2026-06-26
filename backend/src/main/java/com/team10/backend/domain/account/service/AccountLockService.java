package com.team10.backend.domain.account.service;

import com.team10.backend.domain.account.entity.Account;
import com.team10.backend.domain.account.exception.AccountErrorCode;
import com.team10.backend.domain.account.repository.AccountRepository;
import com.team10.backend.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountLockService {

    private final AccountRepository accountRepository;

    public Account findByIdForUpdate(Long accountId) {
        return accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new BusinessException(AccountErrorCode.ACCOUNT_NOT_FOUND));
    }

    public LockedAccounts lockTwoAccounts(Account firstAccount, Account secondAccount) {
        Long firstAccountId = firstAccount.getId();
        Long secondAccountId = secondAccount.getId();

        if (firstAccountId < secondAccountId) {
            Account lockedFirstAccount = findByIdForUpdate(firstAccountId);
            Account lockedSecondAccount = findByIdForUpdate(secondAccountId);
            return new LockedAccounts(lockedFirstAccount, lockedSecondAccount);
        }

        Account lockedSecondAccount = findByIdForUpdate(secondAccountId);
        Account lockedFirstAccount = findByIdForUpdate(firstAccountId);
        return new LockedAccounts(lockedFirstAccount, lockedSecondAccount);
    }

    public record LockedAccounts(Account firstAccount, Account secondAccount) {
    }
}
