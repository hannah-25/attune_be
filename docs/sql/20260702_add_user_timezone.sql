-- User timezone support for local-wall-clock medication alarms (MySQL 8.4)
-- Run before deploying the timezone-aware medication alarm backend because prod uses ddl-auto=validate.

ALTER TABLE user_settings
  ADD COLUMN timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Seoul';

CREATE INDEX idx_user_settings_timezone
  ON user_settings (timezone);

-- 복합 인덱스는 동등 조건(is_active =) 컬럼을 범위 조건(dose_time BETWEEN) 컬럼보다 앞에 둔다.
-- 알림 쿼리가 항상 is_active = true로 필터하므로 (is_active, dose_time) 순서가 dose_time 범위를 연속 스캔한다.
CREATE INDEX idx_user_medication_schedules_active_dose_time
  ON user_medication_schedules (is_active, dose_time);
