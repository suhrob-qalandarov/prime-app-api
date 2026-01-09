-- Update category status enum values
-- Step 1: Drop old check constraint
ALTER TABLE category DROP CONSTRAINT IF EXISTS category_status_check;

-- Step 2: Update VISIBLE to ACTIVE
UPDATE category
SET status = 'ACTIVE'
WHERE status = 'VISIBLE';

-- Step 3: Add new check constraint with updated values (CREATED, ACTIVE, INACTIVE)
ALTER TABLE category
ADD CONSTRAINT category_status_check
CHECK (status IN ('CREATED', 'ACTIVE', 'INACTIVE'));
