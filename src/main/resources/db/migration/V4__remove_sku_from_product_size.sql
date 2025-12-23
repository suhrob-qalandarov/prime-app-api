-- Migration: Remove SKU column and constraint from product_size table
-- This migration removes the SKU column and its unique constraint from product_size table

DO $$
BEGIN
    RAISE NOTICE 'Starting V4 migration: Removing SKU column from product_size table...';

    -- Drop unique constraint on sku column if it exists
    IF EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_name = 'product_size'
        AND constraint_name = 'product_size_sku_key'
    ) THEN
        ALTER TABLE product_size DROP CONSTRAINT product_size_sku_key;
        RAISE NOTICE 'Dropped unique constraint product_size_sku_key';
    END IF;

    -- Drop SKU column if it exists
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'product_size'
        AND column_name = 'sku'
    ) THEN
        ALTER TABLE product_size DROP COLUMN sku;
        RAISE NOTICE 'Dropped SKU column from product_size table';
    ELSE
        RAISE NOTICE 'SKU column does not exist, skipping drop';
    END IF;

    RAISE NOTICE 'Migration V4 completed successfully';
END $$;

