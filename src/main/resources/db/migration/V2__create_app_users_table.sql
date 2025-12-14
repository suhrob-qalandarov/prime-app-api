-- Migration: Create app_users table and migrate data from users
-- This migration:
-- 1. Creates app_users table with same structure as users
-- 2. Copies all data from users to app_users (if users table exists)
-- 3. Updates all foreign key constraints to reference app_users
-- 4. Drops the old users table

-- Step 1: Create app_users table with same structure as users
CREATE TABLE IF NOT EXISTS app_users (
    id BIGSERIAL PRIMARY KEY,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    telegram_id BIGINT,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    tg_username VARCHAR(255),
    phone VARCHAR(255),
    message_id INTEGER,
    verify_code INTEGER,
    verify_code_expiration TIMESTAMP,
    attachment_token VARCHAR(500),
    access_token VARCHAR(1000)
);

-- Step 2: Copy all data from users to app_users (if users table exists)
DO $$
DECLARE
    r RECORD;
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'users') THEN
        -- Drop old foreign key constraints (must be done before copying data)
        FOR r IN (
            SELECT conname, conrelid::regclass AS table_name
            FROM pg_constraint
            WHERE confrelid = 'public.users'::regclass
            AND contype = 'f'
        ) LOOP
            BEGIN
                EXECUTE 'ALTER TABLE ' || r.table_name || ' DROP CONSTRAINT IF EXISTS ' || r.conname;
            EXCEPTION WHEN OTHERS THEN
                NULL;
            END;
        END LOOP;
        
        -- Copy data from users to app_users
        INSERT INTO app_users (
            id, active, created_at, updated_at, created_by, updated_by,
            telegram_id, first_name, last_name, tg_username, phone,
            message_id, verify_code, verify_code_expiration,
            attachment_token, access_token
        )
        SELECT 
            id, active, created_at, updated_at, created_by, updated_by,
            telegram_id, first_name, last_name, tg_username, phone,
            message_id, verify_code, verify_code_expiration,
            attachment_token, access_token
        FROM users
        WHERE NOT EXISTS (SELECT 1 FROM app_users WHERE app_users.id = users.id);
    END IF;
END $$;

-- Step 3: Update foreign key constraints to point to app_users
DO $$
DECLARE
    r RECORD;
    col_name TEXT;
BEGIN
    -- Update user_ip_infos
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'user_ip_infos') THEN
        FOR r IN (
            SELECT conname FROM pg_constraint 
            WHERE conrelid = 'public.user_ip_infos'::regclass 
            AND confrelid = 'public.users'::regclass
            AND contype = 'f'
        ) LOOP
            BEGIN
                EXECUTE 'ALTER TABLE user_ip_infos DROP CONSTRAINT IF EXISTS ' || r.conname;
            EXCEPTION WHEN OTHERS THEN
                NULL;
            END;
        END LOOP;
        
        IF EXISTS (SELECT FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'user_ip_infos' AND column_name = 'user_id') THEN
            BEGIN
                ALTER TABLE user_ip_infos 
                    ADD CONSTRAINT fk_user_ip_infos_user 
                    FOREIGN KEY (user_id) REFERENCES app_users(id);
            EXCEPTION WHEN duplicate_object THEN
                NULL;
            END;
        END IF;
    END IF;
    
    -- Update orders
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'orders') THEN
        FOR r IN (
            SELECT conname FROM pg_constraint 
            WHERE conrelid = 'public.orders'::regclass 
            AND confrelid = 'public.users'::regclass
            AND contype = 'f'
        ) LOOP
            BEGIN
                EXECUTE 'ALTER TABLE orders DROP CONSTRAINT IF EXISTS ' || r.conname;
            EXCEPTION WHEN OTHERS THEN
                NULL;
            END;
        END LOOP;
        
        IF EXISTS (SELECT FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'orders' AND column_name = 'user_id') THEN
            BEGIN
                ALTER TABLE orders 
                    ADD CONSTRAINT fk_orders_user 
                    FOREIGN KEY (user_id) REFERENCES app_users(id);
            EXCEPTION WHEN duplicate_object THEN
                NULL;
            END;
        END IF;
    END IF;
    
    -- Update product_outcomes
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'product_outcomes') THEN
        FOR r IN (
            SELECT conname FROM pg_constraint 
            WHERE conrelid = 'public.product_outcomes'::regclass 
            AND confrelid = 'public.users'::regclass
            AND contype = 'f'
        ) LOOP
            BEGIN
                EXECUTE 'ALTER TABLE product_outcomes DROP CONSTRAINT IF EXISTS ' || r.conname;
            EXCEPTION WHEN OTHERS THEN
                NULL;
            END;
        END LOOP;
        
        IF EXISTS (SELECT FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'product_outcomes' AND column_name = 'user_id') THEN
            BEGIN
                ALTER TABLE product_outcomes 
                    ADD CONSTRAINT fk_product_outcomes_user 
                    FOREIGN KEY (user_id) REFERENCES app_users(id);
            EXCEPTION WHEN duplicate_object THEN
                NULL;
            END;
        END IF;
    END IF;
    
    -- Update sessions
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'sessions') THEN
        FOR r IN (
            SELECT conname FROM pg_constraint 
            WHERE conrelid = 'public.sessions'::regclass 
            AND confrelid = 'public.users'::regclass
            AND contype = 'f'
        ) LOOP
            BEGIN
                EXECUTE 'ALTER TABLE sessions DROP CONSTRAINT IF EXISTS ' || r.conname;
            EXCEPTION WHEN OTHERS THEN
                NULL;
            END;
        END LOOP;
        
        IF EXISTS (SELECT FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'sessions' AND column_name = 'user_id') THEN
            BEGIN
                ALTER TABLE sessions 
                    ADD CONSTRAINT fk_sessions_user 
                    FOREIGN KEY (user_id) REFERENCES app_users(id);
            EXCEPTION WHEN duplicate_object THEN
                NULL;
            END;
        END IF;
    END IF;
    
    -- Update product_incomes
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'product_incomes') THEN
        FOR r IN (
            SELECT conname FROM pg_constraint 
            WHERE conrelid = 'public.product_incomes'::regclass 
            AND confrelid = 'public.users'::regclass
            AND contype = 'f'
        ) LOOP
            BEGIN
                EXECUTE 'ALTER TABLE product_incomes DROP CONSTRAINT IF EXISTS ' || r.conname;
            EXCEPTION WHEN OTHERS THEN
                NULL;
            END;
        END LOOP;
        
        IF EXISTS (SELECT FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'product_incomes' AND column_name = 'admin_user_id') THEN
            BEGIN
                ALTER TABLE product_incomes 
                    ADD CONSTRAINT fk_product_incomes_admin_user 
                    FOREIGN KEY (admin_user_id) REFERENCES app_users(id);
            EXCEPTION WHEN duplicate_object THEN
                NULL;
            END;
        END IF;
    END IF;
    
    -- Create app_users_roles join table (always create if not exists)
    -- Only create if roles table exists
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'roles') THEN
        CREATE TABLE IF NOT EXISTS app_users_roles (
            user_id BIGINT NOT NULL,
            roles_id BIGINT NOT NULL,
            PRIMARY KEY (user_id, roles_id),
            CONSTRAINT fk_app_users_roles_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
            CONSTRAINT fk_app_users_roles_role FOREIGN KEY (roles_id) REFERENCES roles(id) ON DELETE CASCADE
        );
    END IF;
    
    -- Handle existing join tables: rename or copy data
    -- Check if users_roles exists (Hibernate default naming)
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'users_roles') THEN
        -- Determine column name
        SELECT column_name INTO col_name
        FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'users_roles' 
        AND column_name IN ('users_id', 'user_id')
        LIMIT 1;
        
        -- Copy data based on column name
        IF col_name = 'users_id' THEN
            INSERT INTO app_users_roles (user_id, roles_id)
            SELECT users_id, roles_id FROM users_roles
            ON CONFLICT (user_id, roles_id) DO NOTHING;
        ELSIF col_name = 'user_id' THEN
            INSERT INTO app_users_roles (user_id, roles_id)
            SELECT user_id, roles_id FROM users_roles
            ON CONFLICT (user_id, roles_id) DO NOTHING;
        END IF;
        
        -- Drop old table after copying
        DROP TABLE IF EXISTS users_roles CASCADE;
    -- Check if user_roles exists (alternative naming)
    ELSIF EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'user_roles') THEN
        -- Determine column name
        SELECT column_name INTO col_name
        FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'user_roles' 
        AND column_name IN ('users_id', 'user_id')
        LIMIT 1;
        
        -- Copy data based on column name
        IF col_name = 'users_id' THEN
            INSERT INTO app_users_roles (user_id, roles_id)
            SELECT users_id, roles_id FROM user_roles
            ON CONFLICT (user_id, roles_id) DO NOTHING;
        ELSIF col_name = 'user_id' THEN
            INSERT INTO app_users_roles (user_id, roles_id)
            SELECT user_id, roles_id FROM user_roles
            ON CONFLICT (user_id, roles_id) DO NOTHING;
        END IF;
        
        -- Drop old table after copying
        DROP TABLE IF EXISTS user_roles CASCADE;
    END IF;
END $$;

-- Step 4: Update sequence to continue from max id
DO $$
DECLARE
    max_id BIGINT;
BEGIN
    SELECT COALESCE(MAX(id), 0) INTO max_id FROM app_users;
    IF max_id > 0 THEN
        PERFORM setval('app_users_id_seq', max_id, true);
    END IF;
EXCEPTION WHEN OTHERS THEN
    NULL;
END $$;

-- Step 5: Drop old users table (only if app_users has data and users table exists)
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'users') THEN
        IF EXISTS (SELECT FROM app_users LIMIT 1) THEN
            DROP TABLE IF EXISTS users CASCADE;
        END IF;
    END IF;
EXCEPTION WHEN OTHERS THEN
    NULL;
END $$;
