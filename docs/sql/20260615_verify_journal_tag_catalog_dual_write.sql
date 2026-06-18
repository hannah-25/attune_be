-- Journal tag catalog dual-write verification (MySQL 8.4)
-- Read-only. Every count should be zero unless explicitly noted.

-- Legacy user tags created without a catalog mapping.
SELECT 'condition_tags_without_mapping' AS check_name, COUNT(*) AS failure_count
FROM condition_tags legacy
LEFT JOIN legacy_journal_tag_mapping mapping
  ON mapping.legacy_category = 'CONDITION' AND mapping.legacy_tag_id = legacy.id
WHERE legacy.user_id IS NOT NULL AND mapping.journal_tag_id IS NULL
UNION ALL
SELECT 'side_effect_tags_without_mapping', COUNT(*)
FROM side_effect_tags legacy
LEFT JOIN legacy_journal_tag_mapping mapping
  ON mapping.legacy_category = 'SIDE_EFFECT' AND mapping.legacy_tag_id = legacy.id
WHERE legacy.user_id IS NOT NULL AND mapping.journal_tag_id IS NULL
UNION ALL
SELECT 'trouble_tags_without_mapping', COUNT(*)
FROM trouble_tags legacy
LEFT JOIN legacy_journal_tag_mapping mapping
  ON mapping.legacy_category = 'TROUBLE' AND mapping.legacy_tag_id = legacy.id
WHERE legacy.user_id IS NOT NULL AND mapping.journal_tag_id IS NULL;

-- Mappings without preferences. Every mapped user tag must have one preference row.
SELECT 'mapped_tags_without_preference' AS check_name, COUNT(*) AS failure_count
FROM legacy_journal_tag_mapping mapping
LEFT JOIN user_journal_tag_preferences preference
  ON preference.user_id = mapping.user_id
 AND preference.journal_tag_id = mapping.journal_tag_id
WHERE mapping.user_id IS NOT NULL
  AND preference.journal_tag_id IS NULL;

-- Broken mapping references. FK constraints should also prevent these.
SELECT 'mapping_without_catalog_tag' AS check_name, COUNT(*) AS failure_count
FROM legacy_journal_tag_mapping mapping
LEFT JOIN journal_tags tag ON tag.id = mapping.journal_tag_id
WHERE tag.id IS NULL;

-- Mapping category and user ownership must agree with its catalog tag.
SELECT 'mapping_category_mismatch' AS check_name, COUNT(*) AS failure_count
FROM legacy_journal_tag_mapping mapping
JOIN journal_tags tag ON tag.id = mapping.journal_tag_id
WHERE mapping.legacy_category <> tag.category
UNION ALL
SELECT 'user_mapping_owner_mismatch', COUNT(*)
FROM legacy_journal_tag_mapping mapping
JOIN journal_tags tag ON tag.id = mapping.journal_tag_id
WHERE mapping.user_id IS NOT NULL
  AND tag.scope = 'USER'
  AND NOT (mapping.user_id <=> tag.owner_user_id)
UNION ALL
SELECT 'preference_user_tag_owner_mismatch', COUNT(*)
FROM user_journal_tag_preferences preference
JOIN journal_tags tag ON tag.id = preference.journal_tag_id
WHERE tag.scope = 'USER'
  AND NOT (preference.user_id <=> tag.owner_user_id);

-- Logs written after dual-write activation must have both additive columns.
-- Replace the timestamp before running in each environment.
SET @dual_write_enabled_at = '2026-06-15 00:00:00';

SELECT 'condition_logs_missing_dual_write_columns' AS check_name, COUNT(*) AS failure_count
FROM condition_logs
WHERE checked_at >= @dual_write_enabled_at
  AND (user_id IS NULL OR journal_tag_id IS NULL)
UNION ALL
SELECT 'side_effect_logs_missing_dual_write_columns', COUNT(*)
FROM side_effect_logs
WHERE checked_at >= @dual_write_enabled_at
  AND (user_id IS NULL OR journal_tag_id IS NULL)
UNION ALL
SELECT 'trouble_logs_missing_dual_write_columns', COUNT(*)
FROM trouble_logs
WHERE checked_at >= @dual_write_enabled_at
  AND (user_id IS NULL OR journal_tag_id IS NULL);

-- Additive log columns must agree with the legacy tag mapping.
SELECT 'condition_logs_dual_write_mismatch' AS check_name, COUNT(*) AS failure_count
FROM condition_logs log
LEFT JOIN legacy_journal_tag_mapping mapping
  ON mapping.legacy_category = 'CONDITION'
 AND mapping.legacy_tag_id = log.condition_tag_id
WHERE log.checked_at >= @dual_write_enabled_at
  AND (
    mapping.journal_tag_id IS NULL
    OR NOT (log.user_id <=> mapping.user_id)
    OR NOT (log.journal_tag_id <=> mapping.journal_tag_id)
  )
UNION ALL
SELECT 'side_effect_logs_dual_write_mismatch', COUNT(*)
FROM side_effect_logs log
LEFT JOIN legacy_journal_tag_mapping mapping
  ON mapping.legacy_category = 'SIDE_EFFECT'
 AND mapping.legacy_tag_id = log.side_effect_tag_id
WHERE log.checked_at >= @dual_write_enabled_at
  AND (
    mapping.journal_tag_id IS NULL
    OR NOT (log.user_id <=> mapping.user_id)
    OR NOT (log.journal_tag_id <=> mapping.journal_tag_id)
  )
UNION ALL
SELECT 'trouble_logs_dual_write_mismatch', COUNT(*)
FROM trouble_logs log
LEFT JOIN legacy_journal_tag_mapping mapping
  ON mapping.legacy_category = 'TROUBLE'
 AND mapping.legacy_tag_id = log.trouble_tag_id
WHERE log.checked_at >= @dual_write_enabled_at
  AND (
    mapping.journal_tag_id IS NULL
    OR NOT (log.user_id <=> mapping.user_id)
    OR NOT (log.journal_tag_id <=> mapping.journal_tag_id)
  );

-- Informational counts for monitoring growth and unexpected divergence.
SELECT 'journal_tags' AS metric, COUNT(*) AS row_count FROM journal_tags
UNION ALL SELECT 'user_journal_tag_preferences', COUNT(*) FROM user_journal_tag_preferences
UNION ALL SELECT 'legacy_journal_tag_mapping', COUNT(*) FROM legacy_journal_tag_mapping;
