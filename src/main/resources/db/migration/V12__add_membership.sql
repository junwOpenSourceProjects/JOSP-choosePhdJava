ALTER TABLE user_account ADD COLUMN membership VARCHAR(20) NOT NULL DEFAULT 'free' AFTER role;
