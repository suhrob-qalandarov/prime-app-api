-- Migration: Remove + sign from phone numbers in app_users table
-- This migration removes the + prefix from phone numbers if present

DO $$
DECLARE
    updated_count INTEGER;
BEGIN
    RAISE NOTICE 'Starting V3 migration: Removing + sign from phone numbers...';

    -- Update phone numbers: remove + if present
    UPDATE app_users
    SET phone = SUBSTRING(phone FROM 2)
    WHERE phone IS NOT NULL 
      AND phone != ''
      AND phone LIKE '+%';
    
    GET DIAGNOSTICS updated_count = ROW_COUNT;
    
    RAISE NOTICE 'Updated % phone numbers (removed + sign)', updated_count;
    RAISE NOTICE 'Migration V3 completed successfully';
END $$;

