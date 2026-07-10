-- Performance indexes for the 2nd DB indexing pass.
-- Apply manually to production only after reviewing the measurement evidence in
-- docs/performance/db-indexing-effect-20260710.md.

ALTER TABLE medication_analysis_reports
  ADD INDEX idx_medication_analysis_reports_user_generated (user_id, generated_at),
  ADD INDEX idx_medication_analysis_reports_user_period (user_id, period_start, period_end);

ALTER TABLE admin_audit_logs
  ADD INDEX idx_admin_audit_logs_created_id (created_at, id);

ALTER TABLE consultations
  ADD INDEX idx_consultations_user_deleted_date (user_id, is_deleted, consultation_date);

ALTER TABLE onboarding_symptoms
  ADD INDEX idx_onboarding_symptoms_user_saved (user_id, saved_at);

ALTER TABLE asrs_assessments
  ADD INDEX idx_asrs_assessments_user_completed (user_id, completed_at);
