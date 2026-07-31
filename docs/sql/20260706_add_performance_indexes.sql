-- Performance indexes verified in docs/performance/db-indexing-effect-20260706.md.
-- Apply manually to production before running with ddl-auto=validate.

ALTER TABLE schedules
  ADD INDEX idx_schedules_user_deleted_start_end (user_id, is_deleted, start_time, end_time);

ALTER TABLE todos
  ADD INDEX idx_todos_user_deleted_due_at (user_id, is_deleted, due_at);

ALTER TABLE user_medications
  ADD INDEX idx_user_medications_user_active_created_id (user_id, is_active, created_at, id),
  ADD INDEX idx_user_medications_user_started_end (user_id, started_at, end_at);

ALTER TABLE journal_tag_logs
  ADD INDEX idx_journal_tag_logs_user_date_checked (user_id, journal_date, checked_at);

ALTER TABLE community_boards
  ADD INDEX idx_community_boards_deleted_created (created_at, is_deleted),
  ADD INDEX idx_community_boards_deleted_category_created (is_deleted, post_category, created_at);

ALTER TABLE comments
  ADD INDEX idx_comments_post_deleted_created (post_id, is_deleted, created_at);
