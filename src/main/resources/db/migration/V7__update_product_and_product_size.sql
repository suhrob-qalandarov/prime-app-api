-- Migration V7: Update product and product_size tables to match new entity definitions

-- 1. Update products table
ALTER TABLE product
    RENAME COLUMN discount TO discount_percent;

ALTER TABLE product
    ADD COLUMN IF NOT EXISTS warning_quantity INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS category_name VARCHAR(255);

-- Populate category_name from existing relationships
UPDATE product p
SET category_name = (SELECT c.name FROM category c WHERE c.id = p.category_id);

-- Enforce NOT NULL on category_name after population
ALTER TABLE product
    ALTER COLUMN category_name SET NOT NULL;

-- 2. Update product_size table
ALTER TABLE product_size
    RENAME COLUMN amount TO quantity;

ALTER TABLE product_size
    ADD COLUMN IF NOT EXISTS unit_price NUMERIC(19, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS warning_quantity INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_stock INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS status VARCHAR(50) NOT NULL DEFAULT 'ON_SALE';

-- Populate total_stock from quantity (assumption: initially they are equal)
UPDATE product_size
SET total_stock = quantity;
