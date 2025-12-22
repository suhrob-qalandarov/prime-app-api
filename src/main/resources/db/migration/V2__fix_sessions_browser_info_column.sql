-- Migration: Fix sessions table browser_infos column name to browser_info
-- This migration renames the column to match the entity field name

DO $$
BEGIN
    RAISE NOTICE 'Starting V2 migration: Fixing browser_infos column name...';

    -- Check if column exists and rename it
    IF EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'sessions' 
        AND column_name = 'browser_infos'
    ) THEN
        ALTER TABLE sessions RENAME COLUMN browser_infos TO browser_info;
        RAISE NOTICE 'Renamed browser_infos to browser_info in sessions table';
    ELSE
        RAISE NOTICE 'Column browser_infos does not exist, skipping rename';
    END IF;

    RAISE NOTICE 'Migration V2 completed successfully';
END $$;

