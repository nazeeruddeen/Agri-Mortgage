CREATE TABLE agri_mortgage_applications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    application_number VARCHAR(40) NOT NULL,
    primary_applicant_name VARCHAR(150) NOT NULL,
    primary_applicant_aadhaar VARCHAR(12) NOT NULL,
    primary_applicant_pan VARCHAR(10) NOT NULL,
    primary_monthly_income DECIMAL(15, 2) NOT NULL,
    district VARCHAR(80) NOT NULL,
    taluka VARCHAR(80) NOT NULL,
    village VARCHAR(80) NOT NULL,
    requested_amount DECIMAL(15, 2) NOT NULL,
    requested_tenure_months INT NOT NULL,
    purpose VARCHAR(200) NOT NULL,
    status VARCHAR(30) NOT NULL,
    eligible BIT(1) NOT NULL DEFAULT b'0',
    eligibility_summary VARCHAR(1000) NULL,
    total_land_value DECIMAL(15, 2) NULL,
    combined_income DECIMAL(15, 2) NULL,
    ltv_ratio DECIMAL(5, 4) NULL,
    submitted_at DATETIME NULL,
    sanctioned_at DATETIME NULL,
    disbursed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_agri_mortgage_applications_number UNIQUE (application_number)
);

CREATE INDEX idx_agri_mortgage_applications_status ON agri_mortgage_applications (status);
CREATE INDEX idx_agri_mortgage_applications_district ON agri_mortgage_applications (district);
CREATE INDEX idx_agri_mortgage_applications_taluka ON agri_mortgage_applications (taluka);

CREATE TABLE agri_mortgage_applicants (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    application_id BIGINT NOT NULL,
    applicant_type VARCHAR(30) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    aadhaar VARCHAR(12) NOT NULL,
    pan VARCHAR(10) NOT NULL,
    monthly_income DECIMAL(15, 2) NOT NULL,
    relationship_type VARCHAR(30) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_agri_mortgage_applicants_application FOREIGN KEY (application_id) REFERENCES agri_mortgage_applications (id)
);

CREATE INDEX idx_agri_mortgage_applicants_application ON agri_mortgage_applicants (application_id);

CREATE TABLE agricultural_land_parcels (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    application_id BIGINT NOT NULL,
    survey_number VARCHAR(50) NOT NULL,
    district VARCHAR(80) NOT NULL,
    taluka VARCHAR(80) NOT NULL,
    village VARCHAR(80) NOT NULL,
    area_in_acres DECIMAL(10, 2) NOT NULL,
    land_type VARCHAR(30) NOT NULL,
    market_value DECIMAL(15, 2) NOT NULL,
    govt_circle_rate DECIMAL(15, 2) NOT NULL,
    ownership_status VARCHAR(30) NOT NULL,
    encumbrance_status VARCHAR(30) NOT NULL,
    remarks VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_agricultural_land_parcels_application FOREIGN KEY (application_id) REFERENCES agri_mortgage_applications (id)
);

CREATE INDEX idx_agri_land_parcels_application ON agricultural_land_parcels (application_id);
CREATE INDEX idx_agri_land_parcels_district ON agricultural_land_parcels (district);
CREATE INDEX idx_agri_land_parcels_taluka ON agricultural_land_parcels (taluka);

CREATE TABLE agri_mortgage_application_state_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    application_id BIGINT NOT NULL,
    from_status VARCHAR(30) NULL,
    to_status VARCHAR(30) NOT NULL,
    remarks VARCHAR(500) NULL,
    changed_by VARCHAR(120) NOT NULL,
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_agri_state_history_application FOREIGN KEY (application_id) REFERENCES agri_mortgage_applications (id)
);

CREATE INDEX idx_agri_state_history_application ON agri_mortgage_application_state_history (application_id);
