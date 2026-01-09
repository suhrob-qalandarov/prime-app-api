-- Add delivering_at and confirmed_at columns to orders table
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivering_at TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS confirmed_at TIMESTAMP WITHOUT TIME ZONE;
