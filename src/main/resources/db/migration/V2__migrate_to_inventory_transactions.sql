-- Migration: Rename product_incomes to inventory_transactions and add transaction type
-- This migration:
-- 1. Creates new inventory_transactions table with type field
-- 2. Migrates all data from product_incomes to inventory_transactions
-- 3. Drops old product_incomes table

-- Step 1: Create new inventory_transactions table
CREATE TABLE inventory_transactions (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    
    -- Inventory base fields
    stock_quantity INTEGER NOT NULL,
    unit_price NUMERIC(19, 2) NOT NULL DEFAULT 0,
    total_price NUMERIC(19, 2) NOT NULL DEFAULT 0,
    product_id BIGINT NOT NULL,
    
    -- Transaction specific fields
    type VARCHAR(20) NOT NULL DEFAULT 'INCOME',
    admin_user_id BIGINT,
    is_calculated BOOLEAN NOT NULL DEFAULT false,
    selling_price NUMERIC(19, 2),
    notes VARCHAR(500),
    
    -- Constraints
    CONSTRAINT fk_inventory_transaction_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_inventory_transaction_admin_user FOREIGN KEY (admin_user_id) REFERENCES app_users(id) ON DELETE SET NULL
);

-- Step 2: Create indexes for better performance
CREATE INDEX idx_inventory_transaction_product_id ON inventory_transactions(product_id);
CREATE INDEX idx_inventory_transaction_type ON inventory_transactions(type);
CREATE INDEX idx_inventory_transaction_created_at ON inventory_transactions(created_at);
CREATE INDEX idx_inventory_transaction_admin_user_id ON inventory_transactions(admin_user_id);

-- Step 3: Migrate data from product_incomes to inventory_transactions
INSERT INTO inventory_transactions (
    id,
    created_at,
    updated_at,
    created_by,
    updated_by,
    stock_quantity,
    unit_price,
    total_price,
    product_id,
    type,
    admin_user_id,
    is_calculated,
    selling_price,
    notes
)
SELECT 
    id,
    created_at,
    updated_at,
    created_by,
    updated_by,
    stock_quantity,
    unit_price,
    total_price,
    product_id,
    'INCOME' as type,  -- All existing records are INCOME
    admin_user_id,
    is_calculated,
    selling_price,
    NULL as notes
FROM product_incomes;

-- Step 4: Update sequence to continue from the last ID
SELECT setval('inventory_transactions_id_seq', (SELECT MAX(id) FROM inventory_transactions));

-- Step 5: Drop old product_incomes table
DROP TABLE IF EXISTS product_incomes CASCADE;

-- Step 6: Add comment to table
COMMENT ON TABLE inventory_transactions IS 'Unified table for both INCOME and OUTCOME warehouse transactions';
COMMENT ON COLUMN inventory_transactions.type IS 'Transaction type: INCOME or OUTCOME';
COMMENT ON COLUMN inventory_transactions.notes IS 'Additional notes, especially useful for OUTCOME transactions (e.g., reason for stock reduction)';
