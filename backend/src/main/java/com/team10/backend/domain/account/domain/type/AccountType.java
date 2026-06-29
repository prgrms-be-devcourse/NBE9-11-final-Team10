package com.team10.backend.domain.account.domain.type;

public enum AccountType {
    DEPOSIT,
    SAVING_DEPOSIT,
    SAVING_INSTALLMENT;

    public boolean canTransferOut() {
        return this == DEPOSIT;
    }
}
