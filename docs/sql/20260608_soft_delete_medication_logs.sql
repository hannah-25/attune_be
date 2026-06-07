-- Soft delete support for medication logs in production (MySQL 8.4)
-- Run before deploying code that validates user_medication_logs.is_active.

ALTER TABLE user_medication_logs
  ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE user_medication_logs
  ADD INDEX idx_user_medication_logs_schedule_active_taken_at (
    user_medication_schedule_id,
    is_active,
    taken_at
  );

ALTER TABLE user_medication_logs
  DROP INDEX idx_user_medication_logs_schedule_id;
