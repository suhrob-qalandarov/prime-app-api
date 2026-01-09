-- Add product status timestamp columns for tracking
ALTER TABLE product 
ADD COLUMN IF NOT EXISTS published_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS last_activated_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS last_out_of_stock_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS last_deactivated_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS last_archived_at TIMESTAMP;
