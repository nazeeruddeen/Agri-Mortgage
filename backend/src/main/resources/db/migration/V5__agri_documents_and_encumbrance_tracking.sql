ALTER TABLE agri_mortgage_applications
    ADD COLUMN encumbrance_verification_status VARCHAR(30) NOT NULL DEFAULT 'NOT_RUN',
    ADD COLUMN encumbrance_verification_summary VARCHAR(1000) NULL,
    ADD COLUMN encumbrance_verified_at DATETIME NULL;

ALTER TABLE agricultural_land_parcels
    ADD COLUMN encumbrance_check_details VARCHAR(500) NULL,
    ADD COLUMN gateway_available BIT(1) NULL,
    ADD COLUMN encumbrance_checked_at DATETIME NULL;

CREATE TABLE agri_mortgage_documents (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    application_id BIGINT NOT NULL,
    document_type VARCHAR(40) NOT NULL,
    document_status VARCHAR(30) NOT NULL,
    file_name VARCHAR(180) NOT NULL,
    file_reference VARCHAR(255) NOT NULL,
    remarks VARCHAR(500) NULL,
    uploaded_by VARCHAR(120) NOT NULL,
    reviewed_by VARCHAR(120) NULL,
    reviewed_at DATETIME NULL,
    uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_agri_mortgage_documents_application FOREIGN KEY (application_id) REFERENCES agri_mortgage_applications (id)
);

CREATE INDEX idx_agri_mortgage_documents_application ON agri_mortgage_documents (application_id);
CREATE INDEX idx_agri_mortgage_documents_type ON agri_mortgage_documents (document_type);
