-- Migration: Remove accessToken and attachmentToken from app_users table
-- This migration removes the accessToken and attachmentToken columns from app_users table
-- as these tokens are now stored in the sessions table

DO $$
BEGIN
    -- Drop accessToken column if it exists
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_schema = 'public' 
               AND table_name = 'app_users' 
               AND column_name = 'access_token') THEN
        ALTER TABLE app_users DROP COLUMN access_token;
        RAISE NOTICE 'Dropped access_token column from app_users table';
    ELSE
        RAISE NOTICE 'Column access_token does not exist in app_users table';
    END IF;

    -- Drop attachmentToken column if it exists
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_schema = 'public' 
               AND table_name = 'app_users' 
               AND column_name = 'attachment_token') THEN
        ALTER TABLE app_users DROP COLUMN attachment_token;
        RAISE NOTICE 'Dropped attachment_token column from app_users table';
    ELSE
        RAISE NOTICE 'Column attachment_token does not exist in app_users table';
    END IF;

    RAISE NOTICE 'Migration V7 completed successfully';
END $$;

