-- Migration: Drop users_roles and user_roles tables
-- This migration ensures that old users_roles and user_roles tables are completely removed
-- Data should already be migrated to app_users_roles in V5 migration

DO $$
DECLARE
    constraint_name TEXT;
    index_name TEXT;
BEGIN
    -- Drop users_roles table if it still exists
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'users_roles') THEN
        RAISE NOTICE 'Found users_roles table, dropping it...';
        
        -- Drop foreign key constraints referencing users_roles (if any)
        FOR constraint_name IN (
            SELECT conname 
            FROM pg_constraint 
            WHERE confrelid = 'public.users_roles'::regclass
        ) LOOP
            BEGIN
                EXECUTE 'ALTER TABLE users_roles DROP CONSTRAINT IF EXISTS ' || constraint_name;
                RAISE NOTICE 'Dropped foreign key constraint % on table users_roles', constraint_name;
            EXCEPTION WHEN OTHERS THEN
                RAISE WARNING 'Could not drop constraint %: %', constraint_name, SQLERRM;
            END;
        END LOOP;

        -- Drop foreign key constraints from users_roles
        FOR constraint_name IN (
            SELECT conname 
            FROM pg_constraint 
            WHERE conrelid = 'public.users_roles'::regclass 
              AND contype = 'f'
        ) LOOP
            BEGIN
                EXECUTE 'ALTER TABLE users_roles DROP CONSTRAINT IF EXISTS ' || constraint_name;
                RAISE NOTICE 'Dropped foreign key constraint % from table users_roles', constraint_name;
            EXCEPTION WHEN OTHERS THEN
                RAISE WARNING 'Could not drop constraint %: %', constraint_name, SQLERRM;
            END;
        END LOOP;

        -- Drop indexes on users_roles
        FOR index_name IN (
            SELECT indexname 
            FROM pg_indexes 
            WHERE tablename = 'users_roles' 
              AND schemaname = 'public'
        ) LOOP
            BEGIN
                EXECUTE 'DROP INDEX IF EXISTS ' || index_name || ' CASCADE';
                RAISE NOTICE 'Dropped index % on table users_roles', index_name;
            EXCEPTION WHEN OTHERS THEN
                RAISE WARNING 'Could not drop index %: %', index_name, SQLERRM;
            END;
        END LOOP;

        -- Drop the users_roles table
        DROP TABLE IF EXISTS users_roles CASCADE;
        RAISE NOTICE 'Table users_roles dropped successfully.';
    ELSE
        RAISE NOTICE 'Table users_roles does not exist, skipping.';
    END IF;

    -- Drop user_roles table if it still exists (alternative naming)
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'user_roles') THEN
        RAISE NOTICE 'Found user_roles table, dropping it...';
        
        -- Drop foreign key constraints referencing user_roles (if any)
        FOR constraint_name IN (
            SELECT conname 
            FROM pg_constraint 
            WHERE confrelid = 'public.user_roles'::regclass
        ) LOOP
            BEGIN
                EXECUTE 'ALTER TABLE user_roles DROP CONSTRAINT IF EXISTS ' || constraint_name;
                RAISE NOTICE 'Dropped foreign key constraint % on table user_roles', constraint_name;
            EXCEPTION WHEN OTHERS THEN
                RAISE WARNING 'Could not drop constraint %: %', constraint_name, SQLERRM;
            END;
        END LOOP;

        -- Drop foreign key constraints from user_roles
        FOR constraint_name IN (
            SELECT conname 
            FROM pg_constraint 
            WHERE conrelid = 'public.user_roles'::regclass 
              AND contype = 'f'
        ) LOOP
            BEGIN
                EXECUTE 'ALTER TABLE user_roles DROP CONSTRAINT IF EXISTS ' || constraint_name;
                RAISE NOTICE 'Dropped foreign key constraint % from table user_roles', constraint_name;
            EXCEPTION WHEN OTHERS THEN
                RAISE WARNING 'Could not drop constraint %: %', constraint_name, SQLERRM;
            END;
        END LOOP;

        -- Drop indexes on user_roles
        FOR index_name IN (
            SELECT indexname 
            FROM pg_indexes 
            WHERE tablename = 'user_roles' 
              AND schemaname = 'public'
        ) LOOP
            BEGIN
                EXECUTE 'DROP INDEX IF EXISTS ' || index_name || ' CASCADE';
                RAISE NOTICE 'Dropped index % on table user_roles', index_name;
            EXCEPTION WHEN OTHERS THEN
                RAISE WARNING 'Could not drop index %: %', index_name, SQLERRM;
            END;
        END LOOP;

        -- Drop the user_roles table
        DROP TABLE IF EXISTS user_roles CASCADE;
        RAISE NOTICE 'Table user_roles dropped successfully.';
    ELSE
        RAISE NOTICE 'Table user_roles does not exist, skipping.';
    END IF;

    RAISE NOTICE 'Migration V9 completed successfully';
END $$;

