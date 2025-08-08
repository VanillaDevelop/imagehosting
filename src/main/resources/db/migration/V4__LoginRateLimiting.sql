ALTER TABLE users 
ADD COLUMN last_login_attempt TIMESTAMP,
ADD COLUMN failed_login_attempts INTEGER DEFAULT 0 NOT NULL;