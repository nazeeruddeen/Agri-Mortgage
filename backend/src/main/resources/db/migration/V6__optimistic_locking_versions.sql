ALTER TABLE agri_mortgage_applications
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE agri_mortgage_applicants
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE agricultural_land_parcels
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE agri_mortgage_documents
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
