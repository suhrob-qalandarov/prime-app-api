ALTER TABLE app_users ADD COLUMN status VARCHAR(20);

UPDATE app_users SET status = 'ACTIVE' WHERE active = true;
UPDATE app_users SET status = 'INACTIVE' WHERE active = false;
UPDATE app_users SET status = 'INACTIVE' WHERE status IS NULL;

ALTER TABLE app_users ALTER COLUMN status SET DEFAULT 'INACTIVE';
ALTER TABLE app_users ALTER COLUMN status SET NOT NULL;

ALTER TABLE app_users DROP COLUMN active;
