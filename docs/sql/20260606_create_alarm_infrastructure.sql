-- Alarm infrastructure for production (MySQL 8.4)
-- Run before deploying the alarm-enabled backend because prod uses ddl-auto=validate.

ALTER TABLE user_settings
  ADD COLUMN community_notification BOOLEAN NOT NULL DEFAULT TRUE,
  ADD COLUMN todo_notification BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE user_medications
  ADD COLUMN alarm_active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE IF NOT EXISTS schedule_alarms (
  id BIGINT NOT NULL AUTO_INCREMENT,
  schedule_id BIGINT NOT NULL,
  alarm_at DATETIME(6) NOT NULL,
  is_sent BOOLEAN NOT NULL DEFAULT FALSE,
  fail_count TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_schedule_alarm_sent_at (is_sent, alarm_at),
  KEY idx_schedule_alarm_schedule_id (schedule_id),
  CONSTRAINT fk_schedule_alarms_schedule
    FOREIGN KEY (schedule_id) REFERENCES schedules(id)
    ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS notification_subscriptions (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BINARY(16) NOT NULL,
  platform VARCHAR(20) NOT NULL,
  provider VARCHAR(20) NOT NULL,
  endpoint VARCHAR(700) NULL,
  p256dh TEXT NULL,
  auth TEXT NULL,
  token VARCHAR(500) NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_notification_subscription_user_endpoint (user_id, endpoint),
  UNIQUE KEY uk_notification_subscription_user_token (user_id, token),
  KEY idx_notification_subscription_user_enabled (user_id, enabled),
  CONSTRAINT fk_notification_subscriptions_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS notification_history (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BINARY(16) NOT NULL,
  alarm_type VARCHAR(50) NOT NULL,
  reference_id BIGINT NULL,
  alarm_scheduled_at DATETIME(6) NOT NULL,
  title VARCHAR(255) NOT NULL,
  body TEXT NULL,
  status VARCHAR(20) NOT NULL,
  sent_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_notification_history_delivery (
    user_id,
    alarm_type,
    reference_id,
    alarm_scheduled_at
  ),
  KEY idx_notification_history_user_sent_at (user_id, sent_at),
  CONSTRAINT fk_notification_history_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE
) ENGINE=InnoDB;

-- Preserve existing schedule alarms stored as a JSON array of ISO local date-times.
INSERT INTO schedule_alarms (schedule_id, alarm_at, is_sent, fail_count)
SELECT
  s.id,
  CAST(REPLACE(legacy.alarm_at_text, 'T', ' ') AS DATETIME(6)),
  FALSE,
  0
FROM schedules s
JOIN JSON_TABLE(
  CASE WHEN JSON_VALID(s.alarmed_at) THEN s.alarmed_at ELSE JSON_ARRAY() END,
  '$[*]' COLUMNS(alarm_at_text VARCHAR(40) PATH '$')
) legacy
WHERE s.alarm_enabled = TRUE
  AND s.alarmed_at IS NOT NULL
  AND JSON_VALID(s.alarmed_at)
  AND NOT EXISTS (
    SELECT 1
    FROM schedule_alarms sa
    WHERE sa.schedule_id = s.id
      AND sa.alarm_at = CAST(REPLACE(legacy.alarm_at_text, 'T', ' ') AS DATETIME(6))
  );

-- Keep schedules.alarmed_at during this deployment. Drop it only after verifying
-- migrated schedule_alarms rows in production.
