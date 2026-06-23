ALTER TABLE accounts
    ADD COLUMN account_password_hash varchar(255) NULL;

CREATE INDEX idx_deposits_withdraw_account_status_locked
    ON deposits (withdraw_account_id, status, withdrawal_locked);

CREATE INDEX idx_installments_withdraw_account_status_locked
    ON installments (withdraw_account_id, status, withdrawal_locked);