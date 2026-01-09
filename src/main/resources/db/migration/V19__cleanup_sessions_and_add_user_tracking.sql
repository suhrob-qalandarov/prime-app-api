-- Add IP and Browser Info to app_users
ALTER TABLE app_users ADD COLUMN ip_address VARCHAR(255);
ALTER TABLE app_users ADD COLUMN browser_info TEXT;

-- Remove Session references from orders
ALTER TABLE orders DROP COLUMN session_id CASCADE;
ALTER TABLE orders DROP COLUMN admin_session_id CASCADE;

-- Drop sessions table
DROP TABLE IF EXISTS sessions CASCADE;
