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

-- Backfill onboarding_goal_snapshots for existing goals.
-- onboarding_time mirrors saved_at so that getHistoryDetail (which now queries
-- onboarding_goal_snapshots) continues to return goals for pre-migration users.
-- NOT EXISTS guard makes this safe to rerun.
INSERT INTO onboarding_goal_snapshots (user_id, daily_goal_id, onboarding_time)
SELECT goal.user_id, goal.id, goal.saved_at
FROM daily_goals goal
WHERE goal.saved_at IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM onboarding_goal_snapshots snap
    WHERE snap.daily_goal_id = goal.id
  );

COMMIT;

-- Verification: both counts must be zero.
SELECT COUNT(*) AS daily_goals_without_saved_at
FROM daily_goals
WHERE saved_at IS NULL;

SELECT COUNT(*) AS goals_without_snapshot
FROM daily_goals
WHERE saved_at IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM onboarding_goal_snapshots snap
    WHERE snap.daily_goal_id = daily_goals.id
  );
