-- Add timestamp fields to track category activation/deactivation
ALTER TABLE category
ADD COLUMN last_activated_at TIMESTAMP,
ADD COLUMN last_deactivated_at TIMESTAMP;
