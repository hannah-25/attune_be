-- Backfill daily_goals.saved_at for onboarding history compatibility (MySQL 8.4)
-- Prerequisite: the nullable daily_goals.saved_at column must already exist.
-- Safe to rerun: only rows whose saved_at is NULL are updated.
--
-- Legacy daily goals do not contain enough information to reconstruct every
-- historical onboarding round. Assign them to the user's latest completed ASRS
-- assessment so they remain visible in the latest pre-migration history.
-- CURRENT_TIMESTAMP(6) is used only for orphaned goals with no completed ASRS
-- assessment; those rows cannot belong to an existing onboarding history.

START TRANSACTION;

UPDATE daily_goals goal
LEFT JOIN (
  SELECT user_id, MAX(completed_at) AS latest_completed_at
  FROM asrs_assessments
  WHERE completed_at IS NOT NULL
  GROUP BY user_id
) assessment
  ON assessment.user_id = goal.user_id
SET goal.saved_at = COALESCE(assessment.latest_completed_at, CURRENT_TIMESTAMP(6))
WHERE goal.saved_at IS NULL;

COMMIT;

-- Verification: this count must be zero.
SELECT COUNT(*) AS daily_goals_without_saved_at
FROM daily_goals
WHERE saved_at IS NULL;
