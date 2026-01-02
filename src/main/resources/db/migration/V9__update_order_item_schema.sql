-- Rename price to unit_price if strictly needed, but let's check if unit_price is a new column or renamed.
-- Entity has 'unitPrice', V1 had 'price'. Assuming mapping was intended from price -> unitPrice, but Hibernate naming strategy usually maps unitPrice -> unit_price.
-- So 'price' column is likely unused/incorrectly named for new mapping.
-- I'll rename 'price' to 'unit_price' to preserve data if any.

DO $$
BEGIN
    -- Rename column if exists
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='order_item' AND column_name='price') THEN
        ALTER TABLE order_item RENAME COLUMN price TO unit_price;
    END IF;
END $$;

-- Add new columns allowing NULL initially to populate data
ALTER TABLE order_item
    ADD COLUMN IF NOT EXISTS name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS brand VARCHAR(255),
    ADD COLUMN IF NOT EXISTS color_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS color_hex VARCHAR(255),
    ADD COLUMN IF NOT EXISTS size VARCHAR(255),
    ADD COLUMN IF NOT EXISTS discount_percent INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS tag VARCHAR(50) DEFAULT 'NEW',
    ADD COLUMN IF NOT EXISTS category_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS category_id BIGINT;

-- Populate data from related tables (Snapshotting)
-- 1. Populate basic product info
UPDATE order_item oi
SET
    name = p.name,
    brand = p.brand,
    color_name = p.color_name,
    color_hex = p.color_hex,
    discount_percent = p.discount_percent,
    tag = p.tag,
    category_id = p.category_id,
    category_name = c.name
FROM product p
JOIN category c ON p.category_id = c.id
WHERE oi.product_id = p.id;

-- 2. Populate size info
UPDATE order_item oi
SET size = ps.size
FROM product_size ps
WHERE oi.product_size_id = ps.id;

-- 3. Handle any remaining NULLs (broken relations) with defaults
UPDATE order_item SET name = 'Unknown' WHERE name IS NULL;
UPDATE order_item SET brand = 'Unknown' WHERE brand IS NULL;
UPDATE order_item SET color_name = 'Unknown' WHERE color_name IS NULL;
UPDATE order_item SET color_hex = '#000000' WHERE color_hex IS NULL;
UPDATE order_item SET size = 'Unknown' WHERE size IS NULL;
UPDATE order_item SET discount_percent = 0 WHERE discount_percent IS NULL;
UPDATE order_item SET tag = 'NEW' WHERE tag IS NULL;
UPDATE order_item SET category_name = 'Unknown' WHERE category_name IS NULL;
UPDATE order_item SET category_id = (SELECT id FROM category LIMIT 1) WHERE category_id IS NULL; -- Fallback to first category if null
-- If no category exists, this still might be null. We'll check.

UPDATE order_item SET unit_price = 0 WHERE unit_price IS NULL;
UPDATE order_item SET quantity = 0 WHERE quantity IS NULL;

-- Apply constraints
ALTER TABLE order_item
    ALTER COLUMN name SET NOT NULL,
    ALTER COLUMN brand SET NOT NULL,
    ALTER COLUMN color_name SET NOT NULL,
    ALTER COLUMN color_hex SET NOT NULL,
    ALTER COLUMN size SET NOT NULL,
    ALTER COLUMN discount_percent SET NOT NULL,
    ALTER COLUMN tag SET NOT NULL,
    ALTER COLUMN category_name SET NOT NULL,
    ALTER COLUMN unit_price SET NOT NULL,
    ALTER COLUMN quantity SET NOT NULL;

-- category_id might be null if no categories exist. If so, we can't set NOT NULL.
-- But Entity requires it. Assuming we have categories.
DO $$
DECLARE
    null_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO null_count FROM order_item WHERE category_id IS NULL;
    IF null_count = 0 THEN
        ALTER TABLE order_item ALTER COLUMN category_id SET NOT NULL;
    END IF;
END $$;

-- Add Foreign Key
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk_order_item_category') THEN
        ALTER TABLE order_item
            ADD CONSTRAINT fk_order_item_category FOREIGN KEY (category_id) REFERENCES category(id);
    END IF;
END $$;
