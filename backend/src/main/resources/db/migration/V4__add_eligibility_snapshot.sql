-- Add eligibility_rules_snapshot JSON column to agri_mortgage_applications
-- This enables rule versioning at submission time, a key architectural pattern for domain-heavy systems.
ALTER TABLE agri_mortgage_applications
ADD COLUMN eligibility_rules_snapshot JSON;
