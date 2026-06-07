-- Performance: index for TodoAlarmScheduler's findAlarmCandidates query (runs every minute)
-- Filters: is_alarm_sent, is_deleted, is_completed, is_all_day (all equality = false), then range on due_at
ALTER TABLE todos
  ADD INDEX idx_todos_alarm_lookup (is_alarm_sent, is_deleted, is_completed, is_all_day, due_at);
