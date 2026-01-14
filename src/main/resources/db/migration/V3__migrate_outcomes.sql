-- Migration: Migrate product_outcomes to inventory_transactions
-- This migration:
-- 1. Migrates all data from product_outcomes to inventory_transactions with type 'OUTCOME'
-- 2. Drops old product_outcomes table

-- Step 1: Migrate data from product_outcomes to inventory_transactions
INSERT INTO inventory_transactions (
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
    notes
)
SELECT 
    created_at,
    updated_at,
    created_by,
    updated_by,
    stock_quantity,
    unit_price,
    total_price,
    product_id,
    'OUTCOME' as type,
    user_id as admin_user_id, -- Mapping user_id to admin_user_id
    false as is_calculated, -- Outcomes don't need calculation flag usually, or default to false
    CONCAT('Outcome from legacy table. Product Size ID: ', product_size_id) as notes -- Preserve product_size_id info in notes
FROM product_outcomes;

-- Step 2: Drop old product_outcomes table
DROP TABLE IF EXISTS product_outcomes CASCADE;
