ALTER TABLE accounts
    ADD COLUMN account_password_hash varchar(255) NULL;

CREATE INDEX idx_deposits_withdraw_account_status_locked
    ON deposits (withdraw_account_id, status, withdrawal_locked);

CREATE INDEX idx_installments_withdraw_account_status_locked
    ON installments (withdraw_account_id, status, withdrawal_locked);

CREATE INDEX idx_deposits_status_maturity_date
    ON deposits (status, maturity_date);

CREATE INDEX idx_installments_status_maturity_date
    ON installments (status, maturity_date);

CREATE INDEX idx_installments_status_auto_transfer_next_payment_date
    ON installments (status, auto_transfer_yn, next_payment_date);

CREATE INDEX idx_installments_status_next_payment_retry_date
    ON installments (status, next_payment_retry_date);

CREATE INDEX idx_deposits_user_status
    ON deposits (user_id, status);

CREATE INDEX idx_installments_user_status
    ON installments (user_id, status);

CREATE INDEX idx_accounts_user_status
    ON accounts (user_id, status);
