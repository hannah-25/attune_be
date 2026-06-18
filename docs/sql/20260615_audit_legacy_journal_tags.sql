-- Legacy journal tag audit for production (MySQL 8.4)
-- Read-only. Run and save every result set before catalog migration/backfill.

-- 1) Default tags currently used as copy templates.
SELECT 'CONDITION' AS category, id, condition_name AS tag_name, type AS tag_type, is_active, visible
FROM condition_tags
WHERE user_id IS NULL
UNION ALL
SELECT 'SIDE_EFFECT', id, side_effect, 'NONE', is_active, visible
FROM side_effect_tags
WHERE user_id IS NULL
UNION ALL
SELECT 'TROUBLE', id, trouble, type, is_active, visible
FROM trouble_tags
WHERE user_id IS NULL
ORDER BY category, tag_name, id;

-- 2) Duplicate default tags. This result must be empty before backfill.
SELECT 'CONDITION' AS category, condition_name AS tag_name, type AS tag_type, COUNT(*) AS duplicate_count
FROM condition_tags
WHERE user_id IS NULL
GROUP BY condition_name, type
HAVING COUNT(*) > 1
UNION ALL
SELECT 'SIDE_EFFECT', side_effect, 'NONE', COUNT(*)
FROM side_effect_tags
WHERE user_id IS NULL
GROUP BY side_effect
HAVING COUNT(*) > 1
UNION ALL
SELECT 'TROUBLE', trouble, type, COUNT(*)
FROM trouble_tags
WHERE user_id IS NULL
GROUP BY trouble, type
HAVING COUNT(*) > 1;

-- 3) Duplicate user tags by the identity that will be used during migration.
SELECT 'CONDITION' AS category, user_id, condition_name AS tag_name, type AS tag_type,
       COUNT(*) AS duplicate_count, SUM(is_active) AS active_count, SUM(visible) AS visible_count
FROM condition_tags
WHERE user_id IS NOT NULL
GROUP BY user_id, condition_name, type
HAVING COUNT(*) > 1
UNION ALL
SELECT 'SIDE_EFFECT', user_id, side_effect, 'NONE',
       COUNT(*), SUM(is_active), SUM(visible)
FROM side_effect_tags
WHERE user_id IS NOT NULL
GROUP BY user_id, side_effect
HAVING COUNT(*) > 1
UNION ALL
SELECT 'TROUBLE', user_id, trouble, type,
       COUNT(*), SUM(is_active), SUM(visible)
FROM trouble_tags
WHERE user_id IS NOT NULL
GROUP BY user_id, trouble, type
HAVING COUNT(*) > 1;

-- 4) Active users without any tag rows. Intentionally deleted inactive rows count as existing.
SELECT u.id AS user_id,
       NOT EXISTS (SELECT 1 FROM condition_tags t WHERE t.user_id = u.id) AS missing_condition_tags,
       NOT EXISTS (SELECT 1 FROM side_effect_tags t WHERE t.user_id = u.id) AS missing_side_effect_tags,
       NOT EXISTS (SELECT 1 FROM trouble_tags t WHERE t.user_id = u.id) AS missing_trouble_tags
FROM users u
WHERE u.user_status = 'ACTIVE'
  AND (
      NOT EXISTS (SELECT 1 FROM condition_tags t WHERE t.user_id = u.id)
      OR NOT EXISTS (SELECT 1 FROM side_effect_tags t WHERE t.user_id = u.id)
      OR NOT EXISTS (SELECT 1 FROM trouble_tags t WHERE t.user_id = u.id)
  );

-- 5) Orphan logs. Every count must be zero before adding log foreign keys.
SELECT 'CONDITION' AS category, COUNT(*) AS orphan_count
FROM condition_logs l
LEFT JOIN condition_tags t ON t.id = l.condition_tag_id
WHERE t.id IS NULL
UNION ALL
SELECT 'SIDE_EFFECT', COUNT(*)
FROM side_effect_logs l
LEFT JOIN side_effect_tags t ON t.id = l.side_effect_tag_id
WHERE t.id IS NULL
UNION ALL
SELECT 'TROUBLE', COUNT(*)
FROM trouble_logs l
LEFT JOIN trouble_tags t ON t.id = l.trouble_tag_id
WHERE t.id IS NULL;

-- 6) Row counts used to verify that backfill does not lose records.
SELECT 'condition_tags' AS table_name, COUNT(*) AS row_count FROM condition_tags
UNION ALL SELECT 'side_effect_tags', COUNT(*) FROM side_effect_tags
UNION ALL SELECT 'trouble_tags', COUNT(*) FROM trouble_tags
UNION ALL SELECT 'condition_logs', COUNT(*) FROM condition_logs
UNION ALL SELECT 'side_effect_logs', COUNT(*) FROM side_effect_logs
UNION ALL SELECT 'trouble_logs', COUNT(*) FROM trouble_logs;
