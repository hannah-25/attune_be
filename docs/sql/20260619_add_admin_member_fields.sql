ALTER TABLE users
    ADD COLUMN created_at DATETIME(6) NULL,
    ADD COLUMN last_login_at DATETIME(6) NULL;

UPDATE users
SET created_at = COALESCE(created_at, UTC_TIMESTAMP(6));

ALTER TABLE users
    MODIFY COLUMN created_at DATETIME(6) NOT NULL,
    ADD INDEX idx_users_status_created_at (user_status, created_at),
    ADD INDEX idx_users_created_at (created_at);
