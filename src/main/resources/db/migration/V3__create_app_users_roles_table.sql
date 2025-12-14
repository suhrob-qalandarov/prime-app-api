-- Migration: Create app_users_roles join table
-- This migration creates the app_users_roles join table for many-to-many relationship

-- Create app_users_roles join table if not exists
-- Only create if roles and app_users tables exist
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'roles')
       AND EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'app_users') THEN
        CREATE TABLE IF NOT EXISTS app_users_roles (
            user_id BIGINT NOT NULL,
            roles_id BIGINT NOT NULL,
            PRIMARY KEY (user_id, roles_id),
            CONSTRAINT fk_app_users_roles_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
            CONSTRAINT fk_app_users_roles_role FOREIGN KEY (roles_id) REFERENCES roles(id) ON DELETE CASCADE
        );
    END IF;
END $$;

-- Copy data from existing join tables if they exist
DO $$
BEGIN
    -- Copy from users_roles if exists
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'users_roles') THEN
        INSERT INTO app_users_roles (user_id, roles_id)
        SELECT 
            CASE 
                WHEN EXISTS (SELECT FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'users_roles' AND column_name = 'users_id') 
                THEN users_id
                ELSE user_id
            END,
            roles_id
        FROM users_roles
        ON CONFLICT (user_id, roles_id) DO NOTHING;
    END IF;
    
    -- Copy from user_roles if exists
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'user_roles') THEN
        INSERT INTO app_users_roles (user_id, roles_id)
        SELECT 
            CASE 
                WHEN EXISTS (SELECT FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'user_roles' AND column_name = 'users_id') 
                THEN users_id
                ELSE user_id
            END,
            roles_id
        FROM user_roles
        ON CONFLICT (user_id, roles_id) DO NOTHING;
    END IF;
EXCEPTION WHEN OTHERS THEN
    NULL;
END $$;

