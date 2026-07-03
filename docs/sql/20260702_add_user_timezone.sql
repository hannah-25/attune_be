-- User timezone support for local-wall-clock medication alarms (MySQL 8.4)
-- Run before deploying the timezone-aware medication alarm backend because prod uses ddl-auto=validate.

ALTER TABLE user_settings
  ADD COLUMN timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Seoul';

CREATE INDEX idx_user_settings_timezone
  ON user_settings (timezone);

CREATE INDEX idx_user_medication_schedules_dose_time_active
  ON user_medication_schedules (dose_time, is_active);
