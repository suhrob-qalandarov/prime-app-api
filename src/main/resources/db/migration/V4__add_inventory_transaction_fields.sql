-- Migration V4: Add new fields to inventory_transactions table
-- This migration adds product snapshot fields and additional metadata to existing table

-- Add product snapshot columns
ALTER TABLE inventory_transactions ADD COLUMN IF NOT EXISTS product_name VARCHAR(255);
ALTER TABLE inventory_transactions ADD COLUMN IF NOT EXISTS product_description TEXT;
ALTER TABLE inventory_transactions ADD COLUMN IF NOT EXISTS product_main_image_url VARCHAR(500);
ALTER TABLE inventory_transactions ADD COLUMN IF NOT EXISTS product_tag_name VARCHAR(50);
ALTER TABLE inventory_transactions ADD COLUMN IF NOT EXISTS product_status_name VARCHAR(50);
ALTER TABLE inventory_transactions ADD COLUMN IF NOT EXISTS product_color_name VARCHAR(100);
ALTER TABLE inventory_transactions ADD COLUMN IF NOT EXISTS product_color_hex VARCHAR(20);
ALTER TABLE inventory_transactions ADD COLUMN IF NOT EXISTS product_category_name VARCHAR(255);
ALTER TABLE inventory_transactions ADD COLUMN IF NOT EXISTS product_size_name VARCHAR(50);

-- Add discount and note columns
ALTER TABLE inventory_transactions ADD COLUMN IF NOT EXISTS discount_percent INTEGER DEFAULT 0;
ALTER TABLE inventory_transactions ADD COLUMN IF NOT EXISTS reason VARCHAR(50);
ALTER TABLE inventory_transactions ADD COLUMN IF NOT EXISTS note TEXT;
ALTER TABLE inventory_transactions ADD COLUMN IF NOT EXISTS returned_at TIMESTAMP;

-- Add foreign key columns for customer, client, order
ALTER TABLE inventory_transactions ADD COLUMN IF NOT EXISTS customer_id BIGINT;
ALTER TABLE inventory_transactions ADD COLUMN IF NOT EXISTS client_id BIGINT;
ALTER TABLE inventory_transactions ADD COLUMN IF NOT EXISTS order_id BIGINT;
ALTER TABLE inventory_transactions ADD COLUMN IF NOT EXISTS performed_admin_id BIGINT;
ALTER TABLE inventory_transactions ADD COLUMN IF NOT EXISTS return_performed_admin_id BIGINT;

-- Rename old admin_user_id to performed_admin_id if it exists
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'inventory_transactions' 
               AND column_name = 'admin_user_id') THEN
        -- Copy data from old column to new if performed_admin_id is null
        UPDATE inventory_transactions 
        SET performed_admin_id = admin_user_id 
        WHERE performed_admin_id IS NULL AND admin_user_id IS NOT NULL;
        
        -- Drop old column
        ALTER TABLE inventory_transactions DROP COLUMN admin_user_id;
    END IF;
END $$;

-- Update type column to use new enum values (IN/OUT instead of INCOME/OUTCOME)
UPDATE inventory_transactions SET type = 'IN' WHERE type = 'INCOME';
UPDATE inventory_transactions SET type = 'OUT' WHERE type = 'OUTCOME';

-- Add constraints
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_discount_percent') THEN
        ALTER TABLE inventory_transactions 
        ADD CONSTRAINT chk_discount_percent CHECK (discount_percent >= 0 AND discount_percent <= 100);
    END IF;
END $$;

-- Add foreign key constraints (only if they don't exist AND referenced tables exist)
DO $$
BEGIN
    -- FK for customer_id (only if customers table exists)
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'customers') THEN
        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_inventory_transaction_customer') THEN
            ALTER TABLE inventory_transactions 
            ADD CONSTRAINT fk_inventory_transaction_customer 
            FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL;
        END IF;
    END IF;
    
    -- FK for client_id (only if customers table exists)
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'customers') THEN
        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_inventory_transaction_client') THEN
            ALTER TABLE inventory_transactions 
            ADD CONSTRAINT fk_inventory_transaction_client 
            FOREIGN KEY (client_id) REFERENCES customers(id) ON DELETE SET NULL;
        END IF;
    END IF;
    
    -- FK for order_id (only if orders table exists)
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'orders') THEN
        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_inventory_transaction_order') THEN
            ALTER TABLE inventory_transactions 
            ADD CONSTRAINT fk_inventory_transaction_order 
            FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE SET NULL;
        END IF;
    END IF;
    
    -- FK for performed_admin_id (app_users should always exist)
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_inventory_transaction_performed_by') THEN
        ALTER TABLE inventory_transactions 
        ADD CONSTRAINT fk_inventory_transaction_performed_by 
        FOREIGN KEY (performed_admin_id) REFERENCES app_users(id) ON DELETE SET NULL;
    END IF;
    
    -- FK for return_performed_admin_id (app_users should always exist)
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_inventory_transaction_return_performed_by') THEN
        ALTER TABLE inventory_transactions 
        ADD CONSTRAINT fk_inventory_transaction_return_performed_by 
        FOREIGN KEY (return_performed_admin_id) REFERENCES app_users(id) ON DELETE SET NULL;
    END IF;
END $$;

-- Create new indexes for filtering
CREATE INDEX IF NOT EXISTS idx_inventory_transaction_reason ON inventory_transactions(reason);
CREATE INDEX IF NOT EXISTS idx_inventory_transaction_customer_id ON inventory_transactions(customer_id);
CREATE INDEX IF NOT EXISTS idx_inventory_transaction_order_id ON inventory_transactions(order_id);
CREATE INDEX IF NOT EXISTS idx_inventory_transaction_performed_by ON inventory_transactions(performed_admin_id);
CREATE INDEX IF NOT EXISTS idx_inventory_transaction_product_tag ON inventory_transactions(product_tag_name);
CREATE INDEX IF NOT EXISTS idx_inventory_transaction_product_size ON inventory_transactions(product_size_name);
CREATE INDEX IF NOT EXISTS idx_inventory_transaction_product_category ON inventory_transactions(product_category_name);

-- Update existing records with product snapshot data (backfill)
-- This will populate historical data with current product information
UPDATE inventory_transactions t
SET 
    product_name = p.name,
    product_description = COALESCE(p.description, ''),
    product_main_image_url = COALESCE(p.brand, ''),
    product_tag_name = p.tag::VARCHAR,
    product_status_name = p.status::VARCHAR,
    product_color_name = p.color_name,
    product_color_hex = p.color_hex,
    product_category_name = c.name,
    discount_percent = COALESCE(p.discount_percent, 0)
FROM products p
LEFT JOIN categories c ON p.category_id = c.id
WHERE t.product_id = p.id
  AND t.product_name IS NULL;

-- Set default reason for existing transactions based on type
UPDATE inventory_transactions 
SET reason = CASE 
    WHEN type = 'IN' THEN 'PURCHASE'
    WHEN type = 'OUT' THEN 'SALE'
    ELSE 'OTHER'
END
WHERE reason IS NULL;

-- Add comments
COMMENT ON COLUMN inventory_transactions.product_name IS 'Snapshot of product name at transaction time';
COMMENT ON COLUMN inventory_transactions.product_description IS 'Snapshot of product description at transaction time';
COMMENT ON COLUMN inventory_transactions.product_category_name IS 'Snapshot of category name at transaction time';
COMMENT ON COLUMN inventory_transactions.reason IS 'Transaction reason: SALE, PURCHASE, RETURN, etc.';
COMMENT ON COLUMN inventory_transactions.note IS 'Additional notes for the transaction';
COMMENT ON COLUMN inventory_transactions.customer_id IS 'Customer involved in the transaction (for sales)';
COMMENT ON COLUMN inventory_transactions.order_id IS 'Related order ID (if applicable)';
