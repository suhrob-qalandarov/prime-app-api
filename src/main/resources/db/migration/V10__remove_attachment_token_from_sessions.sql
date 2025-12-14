-- Migration: Remove attachment_token column from sessions table
-- This migration removes the attachment_token column from sessions table
-- as attachment tokens are now handled via globalToken

DO $$
BEGIN
    -- Drop attachment_token column if it exists
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'public'
               AND table_name = 'sessions'
               AND column_name = 'attachment_token') THEN
        ALTER TABLE sessions DROP COLUMN attachment_token;
        RAISE NOTICE 'Dropped attachment_token column from sessions table';
    ELSE
        RAISE NOTICE 'Column attachment_token does not exist in sessions table';
    END IF;

    RAISE NOTICE 'Migration V10 completed successfully';
END $$;

