ALTER TABLE accounts
    MODIFY COLUMN account_type enum('DEPOSIT', 'SAVING_DEPOSIT', 'SAVING_INSTALLMENT') NOT NULL;

ALTER TABLE deposits
    ADD COLUMN saving_account_id bigint NULL;

ALTER TABLE deposits
    ADD CONSTRAINT fk_deposits_saving_account
        FOREIGN KEY (saving_account_id) REFERENCES accounts (id);

CREATE INDEX idx_deposits_saving_account_id
    ON deposits (saving_account_id);

ALTER TABLE installments
    ADD COLUMN saving_account_id bigint NULL;

ALTER TABLE installments
    ADD CONSTRAINT fk_installments_saving_account
        FOREIGN KEY (saving_account_id) REFERENCES accounts (id);

CREATE INDEX idx_installments_saving_account_id
    ON installments (saving_account_id);
