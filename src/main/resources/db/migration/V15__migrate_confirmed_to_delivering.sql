-- Migration to handle the new DELIVERING status
-- First, drop the check constraint that might prevent adding the new status
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_status_check;

-- Since the label for CONFIRMED was previously "YETKAZIB BERILAYAPTI" and is now "TASDIQLANGAN",
-- and a new DELIVERING status was added with the "YETKAZIB BERILAYAPTI" label,
-- we migrate existing CONFIRMED orders to DELIVERING to maintain their original meaning.
UPDATE orders SET status = 'DELIVERING' WHERE status = 'CONFIRMED';
