-- 1. Create app_customers table
CREATE TABLE IF NOT EXISTS app_customers (
    id BIGSERIAL PRIMARY KEY,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    full_name TEXT NOT NULL,
    phone_number VARCHAR(255) NOT NULL,
    order_amount INTEGER DEFAULT 0 NOT NULL,
    is_new BOOLEAN DEFAULT TRUE NOT NULL,
    user_profile_id BIGINT NOT NULL,
    CONSTRAINT fk_customers_user FOREIGN KEY (user_profile_id) REFERENCES app_users(id)
);

-- 2. Populate app_customers from app_users
-- Only insert if not exists (though table is new, safe check)
INSERT INTO app_customers (active, full_name, phone_number, order_amount, is_new, user_profile_id, created_at, updated_at)
SELECT
    true,
    TRIM(CONCAT(u.first_name, ' ', COALESCE(u.last_name, ''))),
    u.phone,
    (SELECT COUNT(*) FROM orders o WHERE o.user_id = u.id),
    CASE WHEN (SELECT COUNT(*) FROM orders o WHERE o.user_id = u.id) = 0 THEN TRUE ELSE FALSE END,
    u.id,
    NOW(),
    NOW()
FROM app_users u
WHERE NOT EXISTS (SELECT 1 FROM app_customers c WHERE c.user_profile_id = u.id);

-- 3. Add columns to orders
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS comment TEXT,
    ADD COLUMN IF NOT EXISTS delivered_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS customer_id BIGINT,
    ADD COLUMN IF NOT EXISTS session_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS admin_id BIGINT,
    ADD COLUMN IF NOT EXISTS admin_session_id VARCHAR(100);

-- 4. Update orders.customer_id based on user_id
UPDATE orders o
SET customer_id = (SELECT c.id FROM app_customers c WHERE c.user_profile_id = o.user_id)
WHERE customer_id IS NULL;

-- 5. Update orders.session_id
-- Try to find the latest session for the user
UPDATE orders o
SET session_id = (SELECT s.session_id FROM sessions s WHERE s.user_id = o.user_id ORDER BY s.last_accessed_at DESC LIMIT 1)
WHERE session_id IS NULL;

-- 6. Enforce Foreign Keys and Constraints
-- customer_id must be NOT NULL after update
ALTER TABLE orders
    ALTER COLUMN customer_id SET NOT NULL;

-- Add Constraints
ALTER TABLE orders
    ADD CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES app_customers(id),
    ADD CONSTRAINT fk_orders_session FOREIGN KEY (session_id) REFERENCES sessions(session_id),
    ADD CONSTRAINT fk_orders_admin FOREIGN KEY (admin_id) REFERENCES app_users(id),
    ADD CONSTRAINT fk_orders_admin_session FOREIGN KEY (admin_session_id) REFERENCES sessions(session_id);

-- 7. Update shipping_type values if necessary (e.g. VIA_POST -> VIA_BTS)
UPDATE orders SET shipping_type = 'VIA_BTS' WHERE shipping_type = 'VIA_POST';
