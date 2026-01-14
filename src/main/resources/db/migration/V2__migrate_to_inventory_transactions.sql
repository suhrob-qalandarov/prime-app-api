-- Migration: Create inventory_transactions table with full schema
-- This migration creates the complete inventory_transactions table with all fields including product snapshots

-- Drop old table if exists
DROP TABLE IF EXISTS inventory_transactions CASCADE;

-- Create inventory_transactions table with complete schema
CREATE TABLE inventory_transactions (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    
    -- Product snapshot fields (denormalized for historical accuracy)
    product_name VARCHAR(255) NOT NULL,
    product_description TEXT NOT NULL,
    product_main_image_url VARCHAR(500) NOT NULL,
    product_tag_name VARCHAR(50),
    product_status_name VARCHAR(50),
    product_color_name VARCHAR(100),
    product_color_hex VARCHAR(20),
    product_category_name VARCHAR(255) NOT NULL,
    product_size_name VARCHAR(50),
    
    -- Transaction quantity and pricing
    stock_quantity INTEGER NOT NULL,
    unit_price NUMERIC(19, 2) NOT NULL DEFAULT 0,
    discount_percent INTEGER NOT NULL DEFAULT 0,
    total_price NUMERIC(19, 2) NOT NULL DEFAULT 0,
    
    -- Transaction metadata
    type VARCHAR(20) NOT NULL,  -- IN or OUT
    reason VARCHAR(50) NOT NULL,  -- SALE, PURCHASE, RETURN, etc.
    note TEXT,
    returned_at TIMESTAMP,
    
    -- Foreign key references
    product_id BIGINT NOT NULL,
    customer_id BIGINT,
    client_id BIGINT,
    order_id BIGINT,
    performed_admin_id BIGINT,
    return_performed_admin_id BIGINT,
    
    -- Constraints
    CONSTRAINT fk_inventory_transaction_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_inventory_transaction_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL,
    CONSTRAINT fk_inventory_transaction_client FOREIGN KEY (client_id) REFERENCES customers(id) ON DELETE SET NULL,
    CONSTRAINT fk_inventory_transaction_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE SET NULL,
    CONSTRAINT fk_inventory_transaction_performed_by FOREIGN KEY (performed_admin_id) REFERENCES app_users(id) ON DELETE SET NULL,
    CONSTRAINT fk_inventory_transaction_return_performed_by FOREIGN KEY (return_performed_admin_id) REFERENCES app_users(id) ON DELETE SET NULL,
    CONSTRAINT chk_discount_percent CHECK (discount_percent >= 0 AND discount_percent <= 100)
);

-- Create indexes for better query performance
CREATE INDEX idx_inventory_transaction_product_id ON inventory_transactions(product_id);
CREATE INDEX idx_inventory_transaction_type ON inventory_transactions(type);
CREATE INDEX idx_inventory_transaction_reason ON inventory_transactions(reason);
CREATE INDEX idx_inventory_transaction_created_at ON inventory_transactions(created_at);
CREATE INDEX idx_inventory_transaction_customer_id ON inventory_transactions(customer_id);
CREATE INDEX idx_inventory_transaction_order_id ON inventory_transactions(order_id);
CREATE INDEX idx_inventory_transaction_performed_by ON inventory_transactions(performed_admin_id);
CREATE INDEX idx_inventory_transaction_product_tag ON inventory_transactions(product_tag_name);
CREATE INDEX idx_inventory_transaction_product_size ON inventory_transactions(product_size_name);
CREATE INDEX idx_inventory_transaction_product_category ON inventory_transactions(product_category_name);

-- Add table and column comments
COMMENT ON TABLE inventory_transactions IS 'Unified table for all inventory transactions (IN/OUT) with product snapshots';
COMMENT ON COLUMN inventory_transactions.type IS 'Transaction type: IN (income) or OUT (outcome/sale)';
COMMENT ON COLUMN inventory_transactions.reason IS 'Transaction reason: SALE, PURCHASE, RETURN, etc.';
COMMENT ON COLUMN inventory_transactions.product_name IS 'Snapshot of product name at transaction time';
COMMENT ON COLUMN inventory_transactions.product_description IS 'Snapshot of product description at transaction time';
COMMENT ON COLUMN inventory_transactions.product_category_name IS 'Snapshot of category name at transaction time';
COMMENT ON COLUMN inventory_transactions.note IS 'Additional notes for the transaction';
COMMENT ON COLUMN inventory_transactions.returned_at IS 'Timestamp when the transaction was returned (if applicable)';
