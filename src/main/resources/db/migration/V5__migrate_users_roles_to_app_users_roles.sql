-- Migration: Migrate users_roles to app_users_roles and drop old table
-- This migration:
-- 1. Ensures app_users_roles table exists
-- 2. Copies all data from users_roles to app_users_roles (if users_roles exists)
-- 3. Drops the old users_roles table

DO $$
DECLARE
    col_name TEXT;
    row_count INTEGER;
BEGIN
    -- Ensure app_users_roles table exists
    IF NOT EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'app_users_roles') THEN
        RAISE NOTICE 'Creating app_users_roles table...';
        CREATE TABLE app_users_roles (
            user_id BIGINT NOT NULL,
            roles_id BIGINT NOT NULL,
            PRIMARY KEY (user_id, roles_id),
            CONSTRAINT fk_app_users_roles_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
            CONSTRAINT fk_app_users_roles_role FOREIGN KEY (roles_id) REFERENCES roles(id) ON DELETE CASCADE
        );
        RAISE NOTICE 'app_users_roles table created successfully.';
    ELSE
        RAISE NOTICE 'app_users_roles table already exists.';
    END IF;

    -- Check if users_roles table exists and migrate data
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'users_roles') THEN
        RAISE NOTICE 'Found users_roles table, starting data migration...';
        
        -- Determine column name (users_id or user_id)
        SELECT column_name INTO col_name
        FROM information_schema.columns
        WHERE table_schema = 'public' 
          AND table_name = 'users_roles' 
          AND column_name IN ('users_id', 'user_id')
        LIMIT 1;
        
        IF col_name IS NULL THEN
            RAISE WARNING 'Could not determine column name in users_roles table. Skipping data migration.';
        ELSE
            RAISE NOTICE 'Detected column name: %', col_name;
            
            -- Copy data based on column name
            IF col_name = 'users_id' THEN
                -- Count existing rows before migration
                SELECT COUNT(*) INTO row_count FROM users_roles;
                RAISE NOTICE 'Found % rows in users_roles table', row_count;
                
                -- Insert data, avoiding conflicts
                INSERT INTO app_users_roles (user_id, roles_id)
                SELECT users_id, roles_id 
                FROM users_roles
                WHERE NOT EXISTS (
                    SELECT 1 FROM app_users_roles 
                    WHERE app_users_roles.user_id = users_roles.users_id 
                      AND app_users_roles.roles_id = users_roles.roles_id
                );
                
                GET DIAGNOSTICS row_count = ROW_COUNT;
                RAISE NOTICE 'Migrated % new rows from users_roles to app_users_roles', row_count;
                
            ELSIF col_name = 'user_id' THEN
                -- Count existing rows before migration
                SELECT COUNT(*) INTO row_count FROM users_roles;
                RAISE NOTICE 'Found % rows in users_roles table', row_count;
                
                -- Insert data, avoiding conflicts
                INSERT INTO app_users_roles (user_id, roles_id)
                SELECT user_id, roles_id 
                FROM users_roles
                WHERE NOT EXISTS (
                    SELECT 1 FROM app_users_roles 
                    WHERE app_users_roles.user_id = users_roles.user_id 
                      AND app_users_roles.roles_id = users_roles.roles_id
                );
                
                GET DIAGNOSTICS row_count = ROW_COUNT;
                RAISE NOTICE 'Migrated % new rows from users_roles to app_users_roles', row_count;
            END IF;
            
            -- Verify data was copied correctly
            SELECT COUNT(*) INTO row_count FROM app_users_roles;
            RAISE NOTICE 'Total rows in app_users_roles after migration: %', row_count;
            
            -- Drop foreign key constraints from users_roles (if any)
            FOR col_name IN (
                SELECT conname
                FROM pg_constraint
                WHERE conrelid = 'public.users_roles'::regclass
                  AND contype = 'f'
            ) LOOP
                BEGIN
                    EXECUTE 'ALTER TABLE users_roles DROP CONSTRAINT IF EXISTS ' || col_name;
                    RAISE NOTICE 'Dropped foreign key constraint: %', col_name;
                EXCEPTION WHEN OTHERS THEN
                    RAISE WARNING 'Could not drop constraint %: %', col_name, SQLERRM;
                END;
            END LOOP;
            
            -- Drop indexes on users_roles (if any)
            FOR col_name IN (
                SELECT indexname
                FROM pg_indexes
                WHERE tablename = 'users_roles'
                  AND schemaname = 'public'
            ) LOOP
                BEGIN
                    EXECUTE 'DROP INDEX IF EXISTS ' || col_name || ' CASCADE';
                    RAISE NOTICE 'Dropped index: %', col_name;
                EXCEPTION WHEN OTHERS THEN
                    RAISE WARNING 'Could not drop index %: %', col_name, SQLERRM;
                END;
            END LOOP;
            
            -- Drop the users_roles table
            DROP TABLE IF EXISTS users_roles CASCADE;
            RAISE NOTICE 'users_roles table dropped successfully.';
        END IF;
    ELSE
        RAISE NOTICE 'users_roles table does not exist. Nothing to migrate.';
    END IF;

    -- Also check for user_roles (alternative naming) and migrate if exists
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'user_roles') THEN
        RAISE NOTICE 'Found user_roles table, starting data migration...';
        
        -- Determine column name (users_id or user_id)
        SELECT column_name INTO col_name
        FROM information_schema.columns
        WHERE table_schema = 'public' 
          AND table_name = 'user_roles' 
          AND column_name IN ('users_id', 'user_id')
        LIMIT 1;
        
        IF col_name IS NULL THEN
            RAISE WARNING 'Could not determine column name in user_roles table. Skipping data migration.';
        ELSE
            RAISE NOTICE 'Detected column name: %', col_name;
            
            -- Copy data based on column name
            IF col_name = 'users_id' THEN
                SELECT COUNT(*) INTO row_count FROM user_roles;
                RAISE NOTICE 'Found % rows in user_roles table', row_count;
                
                INSERT INTO app_users_roles (user_id, roles_id)
                SELECT users_id, roles_id 
                FROM user_roles
                WHERE NOT EXISTS (
                    SELECT 1 FROM app_users_roles 
                    WHERE app_users_roles.user_id = user_roles.users_id 
                      AND app_users_roles.roles_id = user_roles.roles_id
                );
                
                GET DIAGNOSTICS row_count = ROW_COUNT;
                RAISE NOTICE 'Migrated % new rows from user_roles to app_users_roles', row_count;
                
            ELSIF col_name = 'user_id' THEN
                SELECT COUNT(*) INTO row_count FROM user_roles;
                RAISE NOTICE 'Found % rows in user_roles table', row_count;
                
                INSERT INTO app_users_roles (user_id, roles_id)
                SELECT user_id, roles_id 
                FROM user_roles
                WHERE NOT EXISTS (
                    SELECT 1 FROM app_users_roles 
                    WHERE app_users_roles.user_id = user_roles.user_id 
                      AND app_users_roles.roles_id = user_roles.roles_id
                );
                
                GET DIAGNOSTICS row_count = ROW_COUNT;
                RAISE NOTICE 'Migrated % new rows from user_roles to app_users_roles', row_count;
            END IF;
            
            -- Drop foreign key constraints from user_roles (if any)
            FOR col_name IN (
                SELECT conname
                FROM pg_constraint
                WHERE conrelid = 'public.user_roles'::regclass
                  AND contype = 'f'
            ) LOOP
                BEGIN
                    EXECUTE 'ALTER TABLE user_roles DROP CONSTRAINT IF EXISTS ' || col_name;
                    RAISE NOTICE 'Dropped foreign key constraint: %', col_name;
                EXCEPTION WHEN OTHERS THEN
                    RAISE WARNING 'Could not drop constraint %: %', col_name, SQLERRM;
                END;
            END LOOP;
            
            -- Drop indexes on user_roles (if any)
            FOR col_name IN (
                SELECT indexname
                FROM pg_indexes
                WHERE tablename = 'user_roles'
                  AND schemaname = 'public'
            ) LOOP
                BEGIN
                    EXECUTE 'DROP INDEX IF EXISTS ' || col_name || ' CASCADE';
                    RAISE NOTICE 'Dropped index: %', col_name;
                EXCEPTION WHEN OTHERS THEN
                    RAISE WARNING 'Could not drop index %: %', col_name, SQLERRM;
                END;
            END LOOP;
            
            -- Drop the user_roles table
            DROP TABLE IF EXISTS user_roles CASCADE;
            RAISE NOTICE 'user_roles table dropped successfully.';
        END IF;
    ELSE
        RAISE NOTICE 'user_roles table does not exist. Nothing to migrate.';
    END IF;

EXCEPTION WHEN OTHERS THEN
    RAISE WARNING 'Error during migration: %', SQLERRM;
    RAISE;
END $$;

