-- Migration: Create all entity tables
-- This migration:
-- 1. Drops all existing entity tables (in reverse dependency order)
-- 2. Creates all entity tables in the correct order to respect foreign key dependencies

DO $$
BEGIN
    RAISE NOTICE 'Starting V1 migration: Dropping existing tables and creating new ones...';

    -- Drop all tables in reverse dependency order (child tables first, then parent tables)
    -- This ensures foreign key constraints don't prevent table deletion

    -- Drop child tables first
    DROP TABLE IF EXISTS order_item CASCADE;
    DROP TABLE IF EXISTS orders CASCADE;
    DROP TABLE IF EXISTS product_outcome CASCADE;
    DROP TABLE IF EXISTS product_income CASCADE;
    DROP TABLE IF EXISTS attachments CASCADE;
    DROP TABLE IF EXISTS product_size CASCADE;
    DROP TABLE IF EXISTS product CASCADE;
    DROP TABLE IF EXISTS category CASCADE;
    DROP TABLE IF EXISTS sessions CASCADE;
    DROP TABLE IF EXISTS app_users_roles CASCADE;
    DROP TABLE IF EXISTS app_users CASCADE;
    DROP TABLE IF EXISTS roles CASCADE;
    DROP TABLE IF EXISTS settings CASCADE;
    DROP TABLE IF EXISTS activities CASCADE;

    -- Also drop any legacy tables that might exist
    DROP TABLE IF EXISTS users_roles CASCADE;
    DROP TABLE IF EXISTS user_roles CASCADE;
    DROP TABLE IF EXISTS users CASCADE;
    DROP TABLE IF EXISTS user_ip_infos CASCADE;

    RAISE NOTICE 'Dropped all existing tables';

    -- Now create all tables in correct dependency order

    -- 1. Create roles table (no dependencies)
    CREATE TABLE roles (
        id BIGSERIAL PRIMARY KEY,
        active BOOLEAN NOT NULL DEFAULT true,
        created_at TIMESTAMP,
        updated_at TIMESTAMP,
        created_by VARCHAR(255),
        updated_by VARCHAR(255),
        name VARCHAR(255) NOT NULL UNIQUE
    );
    CREATE INDEX idx_roles_name ON roles(name);
    RAISE NOTICE 'Created roles table';

    -- 2. Create app_users table (no dependencies)
    CREATE TABLE app_users (
        id BIGSERIAL PRIMARY KEY,
        active BOOLEAN NOT NULL DEFAULT true,
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
        verify_code_expiration TIMESTAMP
    );
    RAISE NOTICE 'Created app_users table';

    -- 3. Create app_users_roles join table (depends on app_users and roles)
    CREATE TABLE app_users_roles (
        user_id BIGINT NOT NULL,
        roles_id BIGINT NOT NULL,
        PRIMARY KEY (user_id, roles_id),
        CONSTRAINT fk_app_users_roles_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
        CONSTRAINT fk_app_users_roles_role FOREIGN KEY (roles_id) REFERENCES roles(id) ON DELETE CASCADE
    );
    RAISE NOTICE 'Created app_users_roles table';

    -- 4. Create categories table (no dependencies)
    CREATE TABLE category (
        id BIGSERIAL PRIMARY KEY,
        active BOOLEAN NOT NULL DEFAULT true,
        created_at TIMESTAMP,
        updated_at TIMESTAMP,
        created_by VARCHAR(255),
        updated_by VARCHAR(255),
        name VARCHAR(255) NOT NULL,
        order_number BIGINT NOT NULL,
        spotlight_name VARCHAR(255),
        status VARCHAR(50) NOT NULL DEFAULT 'CREATED'
    );
    RAISE NOTICE 'Created category table';

    -- 5. Create products table (depends on category)
    CREATE TABLE product (
        id BIGSERIAL PRIMARY KEY,
        active BOOLEAN NOT NULL DEFAULT true,
        created_at TIMESTAMP,
        updated_at TIMESTAMP,
        created_by VARCHAR(255),
        updated_by VARCHAR(255),
        name VARCHAR(255) NOT NULL,
        color_name VARCHAR(255) NOT NULL,
        color_hex VARCHAR(255) NOT NULL,
        brand VARCHAR(255) NOT NULL,
        description TEXT NOT NULL,
        price NUMERIC(19, 2) NOT NULL DEFAULT 0,
        discount INTEGER NOT NULL DEFAULT 0,
        sales_count INTEGER NOT NULL DEFAULT 0,
        tag VARCHAR(50) NOT NULL DEFAULT 'NEW',
        status VARCHAR(50) NOT NULL DEFAULT 'PENDING_INCOME',
        category_id BIGINT NOT NULL,
        CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE CASCADE
    );
    CREATE INDEX idx_product_category_id ON product(category_id);
    RAISE NOTICE 'Created product table';

    -- 6. Create product_sizes table (depends on product)
    CREATE TABLE product_size (
        id BIGSERIAL PRIMARY KEY,
        active BOOLEAN NOT NULL DEFAULT true,
        created_at TIMESTAMP,
        updated_at TIMESTAMP,
        created_by VARCHAR(255),
        updated_by VARCHAR(255),
        amount INTEGER NOT NULL,
        sku VARCHAR(50) NOT NULL UNIQUE,
        size VARCHAR(50) NOT NULL,
        product_id BIGINT NOT NULL,
        cost_price NUMERIC(19, 2) NOT NULL DEFAULT 0,
        version INTEGER NOT NULL DEFAULT 0,
        CONSTRAINT fk_product_size_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,
        CONSTRAINT uk_product_size_product_size UNIQUE (product_id, size)
    );
    CREATE INDEX idx_product_size_product_id ON product_size(product_id);
    RAISE NOTICE 'Created product_size table';

    -- 7. Create attachments table (depends on product)
    CREATE TABLE attachments (
        id BIGSERIAL PRIMARY KEY,
        active BOOLEAN NOT NULL DEFAULT true,
        created_at TIMESTAMP,
        updated_at TIMESTAMP,
        created_by VARCHAR(255),
        updated_by VARCHAR(255),
        url VARCHAR(500) NOT NULL UNIQUE,
        file_path VARCHAR(500) NOT NULL,
        filename VARCHAR(255) NOT NULL,
        original_filename VARCHAR(255),
        content_type VARCHAR(100) NOT NULL,
        file_size BIGINT NOT NULL,
        file_extension VARCHAR(50),
        product_id BIGINT,
        CONSTRAINT fk_attachment_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE SET NULL
    );
    CREATE INDEX idx_attachment_url ON attachments(url);
    CREATE INDEX idx_attachment_product_id ON attachments(product_id);
    RAISE NOTICE 'Created attachments table';

    -- 8. Create product_incomes table (depends on product, app_users)
    CREATE TABLE product_income (
        id BIGSERIAL PRIMARY KEY,
        active BOOLEAN NOT NULL DEFAULT true,
        created_at TIMESTAMP,
        updated_at TIMESTAMP,
        created_by VARCHAR(255),
        updated_by VARCHAR(255),
        stock_quantity INTEGER NOT NULL,
        unit_price NUMERIC(19, 2) NOT NULL DEFAULT 0,
        total_price NUMERIC(19, 2) NOT NULL DEFAULT 0,
        product_id BIGINT NOT NULL,
        admin_user_id BIGINT,
        is_calculated BOOLEAN NOT NULL DEFAULT false,
        selling_price NUMERIC(19, 2),
        CONSTRAINT fk_product_income_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,
        CONSTRAINT fk_product_income_admin_user FOREIGN KEY (admin_user_id) REFERENCES app_users(id) ON DELETE SET NULL
    );
    CREATE INDEX idx_product_income_product_id ON product_income(product_id);
    CREATE INDEX idx_product_income_admin_user_id ON product_income(admin_user_id);
    RAISE NOTICE 'Created product_income table';

    -- 9. Create product_outcomes table (depends on product, app_users, product_size)
    CREATE TABLE product_outcome (
        id BIGSERIAL PRIMARY KEY,
        active BOOLEAN NOT NULL DEFAULT true,
        created_at TIMESTAMP,
        updated_at TIMESTAMP,
        created_by VARCHAR(255),
        updated_by VARCHAR(255),
        stock_quantity INTEGER NOT NULL,
        unit_price NUMERIC(19, 2) NOT NULL DEFAULT 0,
        total_price NUMERIC(19, 2) NOT NULL DEFAULT 0,
        product_id BIGINT NOT NULL,
        user_id BIGINT NOT NULL,
        product_size_id BIGINT,
        CONSTRAINT fk_product_outcome_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,
        CONSTRAINT fk_product_outcome_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
        CONSTRAINT fk_product_outcome_product_size FOREIGN KEY (product_size_id) REFERENCES product_size(id) ON DELETE SET NULL
    );
    CREATE INDEX idx_product_outcome_product_id ON product_outcome(product_id);
    CREATE INDEX idx_product_outcome_user_id ON product_outcome(user_id);
    CREATE INDEX idx_product_outcome_product_size_id ON product_outcome(product_size_id);
    RAISE NOTICE 'Created product_outcome table';

    -- 10. Create sessions table (depends on app_users)
    CREATE TABLE sessions (
        session_id VARCHAR(100) PRIMARY KEY,
        user_id BIGINT,
        ip VARCHAR(45) NOT NULL,
        browser_info TEXT,
        access_token VARCHAR(1000),
        is_active BOOLEAN NOT NULL DEFAULT true,
        is_deleted BOOLEAN NOT NULL DEFAULT false,
        is_authenticated BOOLEAN NOT NULL DEFAULT false,
        is_main_session BOOLEAN NOT NULL DEFAULT false,
        last_accessed_at TIMESTAMP NOT NULL,
        migrated_at TIMESTAMP,
        created_at TIMESTAMP,
        updated_at TIMESTAMP,
        created_by VARCHAR(255),
        updated_by VARCHAR(255),
        CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE SET NULL
    );
    CREATE INDEX idx_session_session_id ON sessions(session_id);
    CREATE INDEX idx_session_user_id ON sessions(user_id);
    CREATE INDEX idx_session_ip ON sessions(ip);
    CREATE INDEX idx_session_is_deleted ON sessions(is_deleted);
    RAISE NOTICE 'Created sessions table';

    -- 11. Create settings table (no dependencies)
    CREATE TABLE settings (
        id BIGSERIAL PRIMARY KEY,
        active BOOLEAN NOT NULL DEFAULT true,
        created_at TIMESTAMP,
        updated_at TIMESTAMP,
        created_by VARCHAR(255),
        updated_by VARCHAR(255),
        key VARCHAR(255) NOT NULL UNIQUE,
        value TEXT NOT NULL,
        description TEXT,
        type VARCHAR(50) NOT NULL,
        category VARCHAR(50) NOT NULL DEFAULT 'OTHER',
        display_name VARCHAR(255),
        default_value TEXT,
        is_editable BOOLEAN NOT NULL DEFAULT true,
        is_visible BOOLEAN NOT NULL DEFAULT true,
        validation_rule VARCHAR(500)
    );
    CREATE INDEX idx_setting_key ON settings(key);
    CREATE INDEX idx_setting_category ON settings(category);
    RAISE NOTICE 'Created settings table';

    -- 12. Create orders table (depends on app_users)
    CREATE TABLE orders (
        id BIGSERIAL PRIMARY KEY,
        active BOOLEAN NOT NULL DEFAULT true,
        created_at TIMESTAMP,
        updated_at TIMESTAMP,
        created_by VARCHAR(255),
        updated_by VARCHAR(255),
        user_id BIGINT NOT NULL,
        status VARCHAR(50),
        total_price NUMERIC(19, 2),
        CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE
    );
    CREATE INDEX idx_orders_user_id ON orders(user_id);
    CREATE INDEX idx_orders_status ON orders(status);
    RAISE NOTICE 'Created orders table';

    -- 13. Create order_item table (depends on orders, product, product_size)
    CREATE TABLE order_item (
        id BIGSERIAL PRIMARY KEY,
        active BOOLEAN NOT NULL DEFAULT true,
        created_at TIMESTAMP,
        updated_at TIMESTAMP,
        created_by VARCHAR(255),
        updated_by VARCHAR(255),
        order_id BIGINT NOT NULL,
        product_id BIGINT NOT NULL,
        product_size_id BIGINT NOT NULL,
        quantity INTEGER,
        price NUMERIC(19, 2),
        CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
        CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,
        CONSTRAINT fk_order_item_product_size FOREIGN KEY (product_size_id) REFERENCES product_size(id) ON DELETE CASCADE
    );
    CREATE INDEX idx_order_item_order_id ON order_item(order_id);
    CREATE INDEX idx_order_item_product_id ON order_item(product_id);
    CREATE INDEX idx_order_item_product_size_id ON order_item(product_size_id);
    RAISE NOTICE 'Created order_item table';

    -- 14. Create activities table (no dependencies)
    CREATE TABLE activities (
        id BIGSERIAL PRIMARY KEY,
        active BOOLEAN NOT NULL DEFAULT true,
        created_at TIMESTAMP,
        updated_at TIMESTAMP,
        created_by VARCHAR(255),
        updated_by VARCHAR(255),
        entity_type VARCHAR(50) NOT NULL,
        entity_id BIGINT NOT NULL,
        timestamp TIMESTAMP NOT NULL,
        action VARCHAR(50) NOT NULL,
        description TEXT,
        performed_by VARCHAR(255),
        details TEXT
    );
    CREATE INDEX idx_activity_entity_type ON activities(entity_type);
    CREATE INDEX idx_activity_entity_id ON activities(entity_id);
    CREATE INDEX idx_activity_timestamp ON activities(timestamp);
    CREATE INDEX idx_activity_entity_type_id ON activities(entity_type, entity_id);
    RAISE NOTICE 'Created activities table';

    RAISE NOTICE 'Migration V1 completed successfully - all entity tables created';
END $$;
