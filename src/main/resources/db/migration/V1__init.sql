-- Initial Migration: Create all tables

-- 1. Roles
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    name VARCHAR(255) NOT NULL UNIQUE
);

-- 2. App Users
CREATE TABLE app_users (
    id BIGSERIAL PRIMARY KEY,
    status VARCHAR(20) NOT NULL DEFAULT 'INACTIVE',
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    telegram_id BIGINT UNIQUE NOT NULL,
    first_name TEXT NOT NULL,
    last_name VARCHAR(255),
    tg_username TEXT NOT NULL,
    phone VARCHAR(255),
    message_id INTEGER,
    verify_code INTEGER,
    verify_code_expiration TIMESTAMP,
    ip_address VARCHAR(255),
    browser_info TEXT
);

-- 3. Users Roles
CREATE TABLE app_users_roles (
    user_id BIGINT NOT NULL,
    roles_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, roles_id),
    CONSTRAINT fk_app_users_roles_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
    CONSTRAINT fk_app_users_roles_role FOREIGN KEY (roles_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- 4. Categories
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    order_number BIGINT NOT NULL,
    spotlight_name VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    last_activated_at TIMESTAMP,
    last_deactivated_at TIMESTAMP
);

-- 5. Products
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
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
    discount_percent INTEGER NOT NULL DEFAULT 0,
    sales_count INTEGER NOT NULL DEFAULT 0,
    warning_quantity INTEGER NOT NULL DEFAULT 0,
    tag VARCHAR(50) NOT NULL DEFAULT 'NEW',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_INCOME',
    category_id BIGINT NOT NULL,
    category_name VARCHAR(255) NOT NULL,
    published_at TIMESTAMP,
    last_activated_at TIMESTAMP,
    last_out_of_stock_at TIMESTAMP,
    last_deactivated_at TIMESTAMP,
    last_archived_at TIMESTAMP,
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
);
CREATE INDEX idx_product_category_id ON products(category_id);

-- 6. Product Sizes
CREATE TABLE product_sizes (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    quantity INTEGER NOT NULL,
    warning_quantity INTEGER NOT NULL DEFAULT 0,
    unit_price NUMERIC(19, 2) NOT NULL DEFAULT 0,
    cost_price NUMERIC(19, 2) NOT NULL DEFAULT 0,
    total_stock INTEGER NOT NULL DEFAULT 0,
    size VARCHAR(50) NOT NULL,
    product_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ON_SALE',
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_product_size_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT uk_product_size_product_size UNIQUE (product_id, size)
);
CREATE INDEX idx_product_size_product_id ON product_sizes(product_id);

-- 7. Attachments
CREATE TABLE attachments (
    id BIGSERIAL PRIMARY KEY,
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
    CONSTRAINT fk_attachment_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL
);
CREATE INDEX idx_attachment_product_id ON attachments(product_id);

-- 8. Product Incomes
CREATE TABLE product_incomes (
    id BIGSERIAL PRIMARY KEY,
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
    CONSTRAINT fk_product_income_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_product_income_admin_user FOREIGN KEY (admin_user_id) REFERENCES app_users(id) ON DELETE SET NULL
);

-- 9. Product Outcomes
CREATE TABLE product_outcomes (
    id BIGSERIAL PRIMARY KEY,
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
    CONSTRAINT fk_product_outcome_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_product_outcome_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
    CONSTRAINT fk_product_outcome_product_size FOREIGN KEY (product_size_id) REFERENCES product_sizes(id) ON DELETE SET NULL
);

-- 10. App Customers
CREATE TABLE app_customers (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    full_name TEXT NOT NULL,
    phone_number VARCHAR(255) NOT NULL,
    order_amount INTEGER NOT NULL DEFAULT 0,
    is_new BOOLEAN NOT NULL DEFAULT TRUE,
    user_profile_id BIGINT NOT NULL,
    CONSTRAINT fk_customers_user FOREIGN KEY (user_profile_id) REFERENCES app_users(id)
);

-- 11. Orders
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    user_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    admin_id BIGINT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_PAYMENT',
    total_price NUMERIC(19, 2) NOT NULL,
    shipping_type VARCHAR(50) NOT NULL,
    comment TEXT,
    admin_comment TEXT,
    cancel_reason VARCHAR(50),
    delivered_at TIMESTAMP,
    paid_at TIMESTAMP,
    delivering_at TIMESTAMP,
    confirmed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    refunded_at TIMESTAMP,
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES app_customers(id),
    CONSTRAINT fk_orders_admin FOREIGN KEY (admin_id) REFERENCES app_users(id)
);
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_customer_id ON orders(customer_id);

-- 12. Order Items
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_size_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(19, 2) NOT NULL,
    discount_percent INTEGER NOT NULL DEFAULT 0,
    name VARCHAR(255) NOT NULL,
    brand VARCHAR(255) NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    color_name VARCHAR(255) NOT NULL,
    color_hex VARCHAR(255) NOT NULL,
    size VARCHAR(255) NOT NULL,
    tag VARCHAR(50) NOT NULL,
    category_name VARCHAR(255) NOT NULL,
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_order_item_product_size FOREIGN KEY (product_size_id) REFERENCES product_sizes(id),
    CONSTRAINT fk_order_item_category FOREIGN KEY (category_id) REFERENCES categories(id)
);
