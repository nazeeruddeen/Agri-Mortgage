CREATE TABLE app_users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(120) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    active BIT(1) NOT NULL DEFAULT b'1',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_app_users_username UNIQUE (username)
);

CREATE TABLE refresh_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    token VARCHAR(512) NOT NULL,
    user_id BIGINT NOT NULL,
    expires_at DATETIME NOT NULL,
    revoked BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_refresh_tokens_token UNIQUE (token),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES app_users (id)
);

CREATE INDEX idx_refresh_tokens_user_revoked ON refresh_tokens (user_id, revoked);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);

CREATE TABLE security_audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    action VARCHAR(60) NOT NULL,
    actor_username VARCHAR(120) NOT NULL,
    target_user_id BIGINT NULL,
    target_username VARCHAR(120) NULL,
    details VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_security_audit_logs_created_at ON security_audit_logs (created_at);
CREATE INDEX idx_security_audit_logs_action ON security_audit_logs (action);

CREATE TABLE borrowers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    legal_business_name VARCHAR(150) NOT NULL,
    contact_person_name VARCHAR(120) NOT NULL,
    business_pan VARCHAR(10) NOT NULL,
    gstin VARCHAR(15) NULL,
    email VARCHAR(180) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    industry_type VARCHAR(80) NOT NULL,
    annual_turnover DECIMAL(15, 2) NOT NULL,
    monthly_income DECIMAL(15, 2) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_borrowers_business_pan UNIQUE (business_pan)
);

CREATE INDEX idx_borrowers_business_pan ON borrowers (business_pan);
CREATE INDEX idx_borrowers_business_name ON borrowers (legal_business_name);

CREATE TABLE borrower_addresses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    borrower_id BIGINT NOT NULL,
    address_type VARCHAR(30) NOT NULL,
    line_one VARCHAR(160) NOT NULL,
    line_two VARCHAR(160) NULL,
    city VARCHAR(80) NOT NULL,
    state VARCHAR(80) NOT NULL,
    postal_code VARCHAR(15) NOT NULL,
    country VARCHAR(80) NOT NULL,
    CONSTRAINT fk_borrower_addresses_borrower FOREIGN KEY (borrower_id) REFERENCES borrowers (id)
);

CREATE INDEX idx_borrower_addresses_borrower ON borrower_addresses (borrower_id);

CREATE TABLE loan_products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_code VARCHAR(40) NOT NULL,
    name VARCHAR(120) NOT NULL,
    min_amount DECIMAL(15, 2) NOT NULL,
    max_amount DECIMAL(15, 2) NOT NULL,
    interest_rate DECIMAL(5, 2) NOT NULL,
    tenure_months INT NOT NULL,
    eligibility_criteria JSON NULL,
    active BIT(1) NOT NULL DEFAULT b'1',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_loan_products_code UNIQUE (product_code)
);

CREATE INDEX idx_loan_products_name ON loan_products (name);
CREATE INDEX idx_loan_products_active ON loan_products (active);

CREATE TABLE eligibility_rules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_code VARCHAR(60) NOT NULL,
    rule_expression VARCHAR(120) NOT NULL,
    rule_type VARCHAR(30) NOT NULL,
    min_value DECIMAL(15, 2) NULL,
    max_value DECIMAL(15, 2) NULL,
    rule_value_text VARCHAR(120) NULL,
    active BIT(1) NOT NULL DEFAULT b'1',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_eligibility_rules_code UNIQUE (rule_code)
);

CREATE INDEX idx_eligibility_rules_active ON eligibility_rules (active);

CREATE TABLE loan_applications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    borrower_id BIGINT NOT NULL,
    loan_product_id BIGINT NOT NULL,
    reviewer_user_id BIGINT NULL,
    requested_amount DECIMAL(15, 2) NOT NULL,
    requested_tenure_months INT NOT NULL,
    purpose VARCHAR(200) NOT NULL,
    status VARCHAR(30) NOT NULL,
    eligibility_passed BIT(1) NOT NULL DEFAULT b'0',
    eligibility_summary VARCHAR(1000) NULL,
    submitted_at DATETIME NULL,
    decisioned_at DATETIME NULL,
    decision_remarks VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_loan_applications_borrower FOREIGN KEY (borrower_id) REFERENCES borrowers (id),
    CONSTRAINT fk_loan_applications_product FOREIGN KEY (loan_product_id) REFERENCES loan_products (id),
    CONSTRAINT fk_loan_applications_reviewer FOREIGN KEY (reviewer_user_id) REFERENCES app_users (id)
);

CREATE INDEX idx_loan_applications_status ON loan_applications (status);
CREATE INDEX idx_loan_applications_borrower ON loan_applications (borrower_id);
CREATE INDEX idx_loan_applications_reviewer ON loan_applications (reviewer_user_id);

CREATE TABLE application_status_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    loan_application_id BIGINT NOT NULL,
    from_status VARCHAR(30) NULL,
    to_status VARCHAR(30) NOT NULL,
    remarks VARCHAR(500) NULL,
    changed_by VARCHAR(120) NOT NULL,
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_application_status_history_application FOREIGN KEY (loan_application_id) REFERENCES loan_applications (id)
);

CREATE INDEX idx_application_status_history_application ON application_status_history (loan_application_id);
