-- Migration: Update sessions table
-- This migration:
-- 1. Adds new fields: is_deleted, is_authenticated, is_main_session
-- 2. Removes expires_at column (sessions don't expire, only tokens do)

DO $$
BEGIN
    -- Add is_deleted column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_schema = 'public' 
                   AND table_name = 'sessions' 
                   AND column_name = 'is_deleted') THEN
        ALTER TABLE sessions ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT false;
        RAISE NOTICE 'Added is_deleted column to sessions table';
    ELSE
        RAISE NOTICE 'Column is_deleted already exists in sessions table';
    END IF;

    -- Add is_authenticated column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_schema = 'public' 
                   AND table_name = 'sessions' 
                   AND column_name = 'is_authenticated') THEN
        ALTER TABLE sessions ADD COLUMN is_authenticated BOOLEAN NOT NULL DEFAULT false;
        RAISE NOTICE 'Added is_authenticated column to sessions table';
    ELSE
        RAISE NOTICE 'Column is_authenticated already exists in sessions table';
    END IF;

    -- Add is_main_session column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_schema = 'public' 
                   AND table_name = 'sessions' 
                   AND column_name = 'is_main_session') THEN
        ALTER TABLE sessions ADD COLUMN is_main_session BOOLEAN NOT NULL DEFAULT false;
        RAISE NOTICE 'Added is_main_session column to sessions table';
    ELSE
        RAISE NOTICE 'Column is_main_session already exists in sessions table';
    END IF;

    -- Drop expires_at column if it exists
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_schema = 'public' 
               AND table_name = 'sessions' 
               AND column_name = 'expires_at') THEN
        -- Drop index on expires_at if it exists
        IF EXISTS (SELECT 1 FROM pg_indexes 
                   WHERE tablename = 'sessions' 
                   AND indexname = 'idx_session_expires_at') THEN
            DROP INDEX IF EXISTS idx_session_expires_at;
            RAISE NOTICE 'Dropped index idx_session_expires_at';
        END IF;
        
        ALTER TABLE sessions DROP COLUMN expires_at;
        RAISE NOTICE 'Dropped expires_at column from sessions table';
    ELSE
        RAISE NOTICE 'Column expires_at does not exist in sessions table';
    END IF;

    -- Create index on is_deleted if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM pg_indexes 
                   WHERE tablename = 'sessions' 
                   AND indexname = 'idx_session_is_deleted') THEN
        CREATE INDEX idx_session_is_deleted ON sessions(is_deleted);
        RAISE NOTICE 'Created index idx_session_is_deleted on sessions table';
    ELSE
        RAISE NOTICE 'Index idx_session_is_deleted already exists on sessions table';
    END IF;

    RAISE NOTICE 'Migration V6 completed successfully';
END $$;

