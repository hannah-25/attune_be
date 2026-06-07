-- Schedule alarm recovery support (2026-06-06)
-- Run during a coordinated deployment with schedulers stopped because production uses ddl-auto=validate.
-- Existing rows older than the recovery window are marked sent to prevent replaying the entire historical backlog.

SET @schedule_alarm_recovery_cutoff = CURRENT_TIMESTAMP(6) - INTERVAL 24 HOUR;

ALTER TABLE schedule_alarms
  ADD COLUMN is_sent BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE schedule_alarms
SET is_sent = TRUE
WHERE alarm_at < @schedule_alarm_recovery_cutoff;

ALTER TABLE schedule_alarms
  DROP INDEX idx_schedule_alarm_at,
  ADD INDEX idx_schedule_alarm_sent_at (is_sent, alarm_at);

-- Required for atomic notification claiming and retry coordination.
ALTER TABLE notification_history
  ADD UNIQUE KEY uk_notification_history_delivery (
    user_id,
    alarm_type,
    reference_id,
    alarm_scheduled_at
  );
