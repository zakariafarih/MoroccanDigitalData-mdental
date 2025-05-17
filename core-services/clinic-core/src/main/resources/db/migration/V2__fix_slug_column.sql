-- First add column allowing nulls
ALTER TABLE IF EXISTS clinics ADD COLUMN IF NOT EXISTS slug VARCHAR(255);

-- Update existing records with a default value
UPDATE clinics SET slug = REPLACE(LOWER(name), ' ', '-') WHERE slug IS NULL;

-- Then make the column non-nullable
ALTER TABLE clinics ALTER COLUMN slug SET NOT NULL;

-- Create index if it doesn't exist
CREATE INDEX IF NOT EXISTS idx_clinic_slug ON clinics(slug);