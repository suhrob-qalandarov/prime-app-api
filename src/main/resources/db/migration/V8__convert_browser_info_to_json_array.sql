-- Migration: Convert browser_info from VARCHAR to JSON array (browser_infos)
-- This migration:
-- 1. Adds new browser_infos TEXT column (JSON array)
-- 2. Migrates existing browser_info data to browser_infos (as JSON array)
-- 3. Drops old browser_info column and its index

DO $$
DECLARE
    session_record RECORD;
    browser_info_json TEXT;
BEGIN
    -- Add browser_infos column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_schema = 'public' 
                   AND table_name = 'sessions' 
                   AND column_name = 'browser_infos') THEN
        ALTER TABLE sessions ADD COLUMN browser_infos TEXT;
        RAISE NOTICE 'Added browser_infos column to sessions table';
    ELSE
        RAISE NOTICE 'Column browser_infos already exists in sessions table';
    END IF;

    -- Migrate existing browser_info data to browser_infos (as JSON array)
    FOR session_record IN SELECT session_id, browser_info FROM sessions WHERE browser_info IS NOT NULL AND browser_info != ''
    LOOP
        -- Convert single browser_info to JSON array
        browser_info_json := '["' || REPLACE(session_record.browser_info, '"', '\"') || '"]';
        
        -- Update browser_infos column
        UPDATE sessions 
        SET browser_infos = browser_info_json 
        WHERE session_id = session_record.session_id;
        
        RAISE NOTICE 'Migrated browser_info for session %: %', session_record.session_id, browser_info_json;
    END LOOP;

    -- Drop index on browser_info if it exists
    IF EXISTS (SELECT 1 FROM pg_indexes 
               WHERE tablename = 'sessions' 
               AND indexname = 'idx_session_ip_browser') THEN
        DROP INDEX IF EXISTS idx_session_ip_browser;
        RAISE NOTICE 'Dropped index idx_session_ip_browser';
    END IF;

    -- Drop browser_info column if it exists
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_schema = 'public' 
               AND table_name = 'sessions' 
               AND column_name = 'browser_info') THEN
        ALTER TABLE sessions DROP COLUMN browser_info;
        RAISE NOTICE 'Dropped browser_info column from sessions table';
    ELSE
        RAISE NOTICE 'Column browser_info does not exist in sessions table';
    END IF;

    RAISE NOTICE 'Migration V8 completed successfully';
END $$;

