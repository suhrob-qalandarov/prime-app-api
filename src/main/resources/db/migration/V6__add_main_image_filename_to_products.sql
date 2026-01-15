-- Migration V6: Add main_image_file_name to products table

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS main_image_filename VARCHAR(255);

COMMENT ON COLUMN products.main_image_filename IS 'Filename of the main product image for quick access without JOIN';
