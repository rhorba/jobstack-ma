-- Profile is completed after registration (docs/ux-jobstack.md flow: Register/Login -> Complete profile + upload CV),
-- so full_name cannot be required at row-creation time.
ALTER TABLE candidate_profiles ALTER COLUMN full_name DROP NOT NULL;
