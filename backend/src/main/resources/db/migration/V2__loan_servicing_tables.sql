ALTER TABLE loan_applications
    ADD COLUMN disbursed_at DATETIME NULL;

CREATE TABLE loan_accounts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    loan_application_id BIGINT NOT NULL,
    account_number VARCHAR(40) NOT NULL,
    disbursement_reference VARCHAR(80) NOT NULL,
    principal_amount DECIMAL(15, 2) NOT NULL,
    annual_interest_rate DECIMAL(5, 2) NOT NULL,
    tenure_months INT NOT NULL,
    monthly_installment_amount DECIMAL(15, 2) NOT NULL,
    outstanding_principal DECIMAL(15, 2) NOT NULL,
    disbursed_at DATETIME NOT NULL,
    next_due_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_loan_accounts_application UNIQUE (loan_application_id),
    CONSTRAINT uk_loan_accounts_account_number UNIQUE (account_number),
    CONSTRAINT fk_loan_accounts_application FOREIGN KEY (loan_application_id) REFERENCES loan_applications (id)
);

CREATE INDEX idx_loan_accounts_status ON loan_accounts (status);
CREATE INDEX idx_loan_accounts_account_number ON loan_accounts (account_number);

CREATE TABLE repayment_installments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    loan_account_id BIGINT NOT NULL,
    installment_number INT NOT NULL,
    due_date DATE NOT NULL,
    opening_principal DECIMAL(15, 2) NOT NULL,
    principal_due DECIMAL(15, 2) NOT NULL,
    interest_due DECIMAL(15, 2) NOT NULL,
    principal_paid DECIMAL(15, 2) NOT NULL DEFAULT 0,
    interest_paid DECIMAL(15, 2) NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL,
    paid_at DATETIME NULL,
    remarks VARCHAR(300) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_repayment_installments_account FOREIGN KEY (loan_account_id) REFERENCES loan_accounts (id)
);

CREATE INDEX idx_repayment_installments_account ON repayment_installments (loan_account_id);
CREATE INDEX idx_repayment_installments_due_date ON repayment_installments (due_date);
CREATE INDEX idx_repayment_installments_status ON repayment_installments (status);

CREATE TABLE loan_repayment_transactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    loan_account_id BIGINT NOT NULL,
    installment_id BIGINT NULL,
    transaction_reference VARCHAR(80) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    applied_principal_amount DECIMAL(15, 2) NOT NULL,
    applied_interest_amount DECIMAL(15, 2) NOT NULL,
    payment_mode VARCHAR(30) NOT NULL,
    payment_date DATE NOT NULL,
    notes VARCHAR(500) NULL,
    recorded_by VARCHAR(120) NOT NULL,
    recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_loan_repayment_transactions_reference UNIQUE (transaction_reference),
    CONSTRAINT fk_loan_repayment_transactions_account FOREIGN KEY (loan_account_id) REFERENCES loan_accounts (id),
    CONSTRAINT fk_loan_repayment_transactions_installment FOREIGN KEY (installment_id) REFERENCES repayment_installments (id)
);

CREATE INDEX idx_loan_repayment_transactions_account ON loan_repayment_transactions (loan_account_id);
CREATE INDEX idx_loan_repayment_transactions_recorded_at ON loan_repayment_transactions (recorded_at);
