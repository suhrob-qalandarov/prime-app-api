-- Migration: Drop user_ip_infos table
-- This migration removes the user_ip_infos table as it's no longer needed.
-- IP and browser information is now stored in the sessions table.

-- Step 1: Drop foreign key constraints from user_ip_infos (if any)
DO $$
DECLARE
    r RECORD;
BEGIN
    -- Drop all foreign key constraints that reference user_ip_infos
    FOR r IN (
        SELECT conname, conrelid::regclass AS table_name
        FROM pg_constraint
        WHERE confrelid = 'public.user_ip_infos'::regclass
        AND contype = 'f'
    ) LOOP
        BEGIN
            EXECUTE 'ALTER TABLE ' || r.table_name || ' DROP CONSTRAINT IF EXISTS ' || r.conname;
            RAISE NOTICE 'Dropped foreign key constraint: % from table: %', r.conname, r.table_name;
        EXCEPTION WHEN OTHERS THEN
            RAISE NOTICE 'Error dropping constraint %: %', r.conname, SQLERRM;
        END;
    END LOOP;
    
    -- Drop all foreign key constraints from user_ip_infos to other tables
    FOR r IN (
        SELECT conname, conrelid::regclass AS table_name
        FROM pg_constraint
        WHERE conrelid = 'public.user_ip_infos'::regclass
        AND contype = 'f'
    ) LOOP
        BEGIN
            EXECUTE 'ALTER TABLE user_ip_infos DROP CONSTRAINT IF EXISTS ' || r.conname;
            RAISE NOTICE 'Dropped foreign key constraint: % from user_ip_infos', r.conname;
        EXCEPTION WHEN OTHERS THEN
            RAISE NOTICE 'Error dropping constraint %: %', r.conname, SQLERRM;
        END;
    END LOOP;
END $$;

-- Step 2: Drop indexes on user_ip_infos table (if any)
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN (
        SELECT indexname
        FROM pg_indexes
        WHERE tablename = 'user_ip_infos'
        AND schemaname = 'public'
    ) LOOP
        BEGIN
            EXECUTE 'DROP INDEX IF EXISTS ' || quote_ident(r.indexname) || ' CASCADE';
            RAISE NOTICE 'Dropped index: %', r.indexname;
        EXCEPTION WHEN OTHERS THEN
            RAISE NOTICE 'Error dropping index %: %', r.indexname, SQLERRM;
        END;
    END LOOP;
END $$;

-- Step 3: Drop the user_ip_infos table if it exists
DROP TABLE IF EXISTS user_ip_infos CASCADE;

-- Step 4: Verify table is dropped
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'user_ip_infos') THEN
        RAISE EXCEPTION 'user_ip_infos table still exists after drop attempt';
    ELSE
        RAISE NOTICE 'user_ip_infos table successfully dropped';
    END IF;
END $$;

