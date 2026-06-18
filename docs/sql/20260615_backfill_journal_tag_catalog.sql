-- Backfill legacy journal tags into the shared catalog (MySQL 8.4)
-- Prerequisite: 20260615_create_journal_tag_catalog.sql
-- Safe to rerun. Review 20260615_audit_legacy_journal_tags.sql results first.

START TRANSACTION;

-- 1) Create system catalog tags from legacy default template rows.
INSERT INTO journal_tags (
  category, name, tag_type, scope, owner_user_id, owner_key, is_active, default_visible
)
SELECT category, name, tag_type, scope, owner_user_id, owner_key, is_active, default_visible
FROM (
  SELECT 'CONDITION' AS category, condition_name AS name, type AS tag_type,
         'SYSTEM' AS scope, NULL AS owner_user_id,
         0x00000000000000000000000000000000 AS owner_key,
         MAX(is_active) AS is_active, MAX(visible) AS default_visible
  FROM condition_tags
  WHERE user_id IS NULL
  GROUP BY condition_name, type
) AS new_tags
ON DUPLICATE KEY UPDATE
  is_active = new_tags.is_active,
  default_visible = new_tags.default_visible;

INSERT INTO journal_tags (
  category, name, tag_type, scope, owner_user_id, owner_key, is_active, default_visible
)
SELECT category, name, tag_type, scope, owner_user_id, owner_key, is_active, default_visible
FROM (
  SELECT 'SIDE_EFFECT' AS category, side_effect AS name, 'NONE' AS tag_type,
         'SYSTEM' AS scope, NULL AS owner_user_id,
         0x00000000000000000000000000000000 AS owner_key,
         MAX(is_active) AS is_active, MAX(visible) AS default_visible
  FROM side_effect_tags
  WHERE user_id IS NULL
  GROUP BY side_effect
) AS new_tags
ON DUPLICATE KEY UPDATE
  is_active = new_tags.is_active,
  default_visible = new_tags.default_visible;

INSERT INTO journal_tags (
  category, name, tag_type, scope, owner_user_id, owner_key, is_active, default_visible
)
SELECT category, name, tag_type, scope, owner_user_id, owner_key, is_active, default_visible
FROM (
  SELECT 'TROUBLE' AS category, trouble AS name, type AS tag_type,
         'SYSTEM' AS scope, NULL AS owner_user_id,
         0x00000000000000000000000000000000 AS owner_key,
         MAX(is_active) AS is_active, MAX(visible) AS default_visible
  FROM trouble_tags
  WHERE user_id IS NULL
  GROUP BY trouble, type
) AS new_tags
ON DUPLICATE KEY UPDATE
  is_active = new_tags.is_active,
  default_visible = new_tags.default_visible;

-- 2) Map legacy default template rows to system catalog tags.
INSERT INTO legacy_journal_tag_mapping (legacy_category, legacy_tag_id, user_id, journal_tag_id)
SELECT legacy_category, legacy_tag_id, user_id, journal_tag_id
FROM (
  SELECT 'CONDITION' AS legacy_category, legacy.id AS legacy_tag_id,
         NULL AS user_id, catalog.id AS journal_tag_id
  FROM condition_tags legacy
  JOIN journal_tags catalog
    ON catalog.scope = 'SYSTEM'
   AND catalog.category = 'CONDITION'
   AND catalog.name = legacy.condition_name
   AND catalog.tag_type = legacy.type
  WHERE legacy.user_id IS NULL
) AS new_mappings
ON DUPLICATE KEY UPDATE journal_tag_id = new_mappings.journal_tag_id;

INSERT INTO legacy_journal_tag_mapping (legacy_category, legacy_tag_id, user_id, journal_tag_id)
SELECT legacy_category, legacy_tag_id, user_id, journal_tag_id
FROM (
  SELECT 'SIDE_EFFECT' AS legacy_category, legacy.id AS legacy_tag_id,
         NULL AS user_id, catalog.id AS journal_tag_id
  FROM side_effect_tags legacy
  JOIN journal_tags catalog
    ON catalog.scope = 'SYSTEM'
   AND catalog.category = 'SIDE_EFFECT'
   AND catalog.name = legacy.side_effect
   AND catalog.tag_type = 'NONE'
  WHERE legacy.user_id IS NULL
) AS new_mappings
ON DUPLICATE KEY UPDATE journal_tag_id = new_mappings.journal_tag_id;

INSERT INTO legacy_journal_tag_mapping (legacy_category, legacy_tag_id, user_id, journal_tag_id)
SELECT legacy_category, legacy_tag_id, user_id, journal_tag_id
FROM (
  SELECT 'TROUBLE' AS legacy_category, legacy.id AS legacy_tag_id,
         NULL AS user_id, catalog.id AS journal_tag_id
  FROM trouble_tags legacy
  JOIN journal_tags catalog
    ON catalog.scope = 'SYSTEM'
   AND catalog.category = 'TROUBLE'
   AND catalog.name = legacy.trouble
   AND catalog.tag_type = legacy.type
  WHERE legacy.user_id IS NULL
) AS new_mappings
ON DUPLICATE KEY UPDATE journal_tag_id = new_mappings.journal_tag_id;

-- 3) Create user catalog tags only when no matching system tag exists.
INSERT INTO journal_tags (
  category, name, tag_type, scope, owner_user_id, owner_key, is_active, default_visible
)
SELECT category, name, tag_type, scope, owner_user_id, owner_key, is_active, default_visible
FROM (
  SELECT 'CONDITION' AS category, legacy.condition_name AS name, legacy.type AS tag_type,
         'USER' AS scope, legacy.user_id AS owner_user_id, legacy.user_id AS owner_key,
         TRUE AS is_active, FALSE AS default_visible
  FROM condition_tags legacy
  LEFT JOIN journal_tags system_tag
    ON system_tag.scope = 'SYSTEM'
   AND system_tag.category = 'CONDITION'
   AND system_tag.name = legacy.condition_name
   AND system_tag.tag_type = legacy.type
  WHERE legacy.user_id IS NOT NULL
    AND system_tag.id IS NULL
  GROUP BY legacy.user_id, legacy.condition_name, legacy.type
) AS new_tags
ON DUPLICATE KEY UPDATE name = new_tags.name;

INSERT INTO journal_tags (
  category, name, tag_type, scope, owner_user_id, owner_key, is_active, default_visible
)
SELECT category, name, tag_type, scope, owner_user_id, owner_key, is_active, default_visible
FROM (
  SELECT 'SIDE_EFFECT' AS category, legacy.side_effect AS name, 'NONE' AS tag_type,
         'USER' AS scope, legacy.user_id AS owner_user_id, legacy.user_id AS owner_key,
         TRUE AS is_active, FALSE AS default_visible
  FROM side_effect_tags legacy
  LEFT JOIN journal_tags system_tag
    ON system_tag.scope = 'SYSTEM'
   AND system_tag.category = 'SIDE_EFFECT'
   AND system_tag.name = legacy.side_effect
   AND system_tag.tag_type = 'NONE'
  WHERE legacy.user_id IS NOT NULL
    AND system_tag.id IS NULL
  GROUP BY legacy.user_id, legacy.side_effect
) AS new_tags
ON DUPLICATE KEY UPDATE name = new_tags.name;

INSERT INTO journal_tags (
  category, name, tag_type, scope, owner_user_id, owner_key, is_active, default_visible
)
SELECT category, name, tag_type, scope, owner_user_id, owner_key, is_active, default_visible
FROM (
  SELECT 'TROUBLE' AS category, legacy.trouble AS name, legacy.type AS tag_type,
         'USER' AS scope, legacy.user_id AS owner_user_id, legacy.user_id AS owner_key,
         TRUE AS is_active, FALSE AS default_visible
  FROM trouble_tags legacy
  LEFT JOIN journal_tags system_tag
    ON system_tag.scope = 'SYSTEM'
   AND system_tag.category = 'TROUBLE'
   AND system_tag.name = legacy.trouble
   AND system_tag.tag_type = legacy.type
  WHERE legacy.user_id IS NOT NULL
    AND system_tag.id IS NULL
  GROUP BY legacy.user_id, legacy.trouble, legacy.type
) AS new_tags
ON DUPLICATE KEY UPDATE name = new_tags.name;

-- 4) Map every legacy user tag to a system tag when possible, otherwise its user tag.
INSERT INTO legacy_journal_tag_mapping (legacy_category, legacy_tag_id, user_id, journal_tag_id)
SELECT legacy_category, legacy_tag_id, user_id, journal_tag_id
FROM (
  SELECT 'CONDITION' AS legacy_category, legacy.id AS legacy_tag_id,
         legacy.user_id, COALESCE(system_tag.id, user_tag.id) AS journal_tag_id
  FROM condition_tags legacy
  LEFT JOIN journal_tags system_tag
    ON system_tag.scope = 'SYSTEM'
   AND system_tag.category = 'CONDITION'
   AND system_tag.name = legacy.condition_name
   AND system_tag.tag_type = legacy.type
  LEFT JOIN journal_tags user_tag
    ON user_tag.scope = 'USER'
   AND user_tag.owner_user_id = legacy.user_id
   AND user_tag.category = 'CONDITION'
   AND user_tag.name = legacy.condition_name
   AND user_tag.tag_type = legacy.type
  WHERE legacy.user_id IS NOT NULL
) AS new_mappings
ON DUPLICATE KEY UPDATE
  user_id = new_mappings.user_id,
  journal_tag_id = new_mappings.journal_tag_id;

INSERT INTO legacy_journal_tag_mapping (legacy_category, legacy_tag_id, user_id, journal_tag_id)
SELECT legacy_category, legacy_tag_id, user_id, journal_tag_id
FROM (
  SELECT 'SIDE_EFFECT' AS legacy_category, legacy.id AS legacy_tag_id,
         legacy.user_id, COALESCE(system_tag.id, user_tag.id) AS journal_tag_id
  FROM side_effect_tags legacy
  LEFT JOIN journal_tags system_tag
    ON system_tag.scope = 'SYSTEM'
   AND system_tag.category = 'SIDE_EFFECT'
   AND system_tag.name = legacy.side_effect
   AND system_tag.tag_type = 'NONE'
  LEFT JOIN journal_tags user_tag
    ON user_tag.scope = 'USER'
   AND user_tag.owner_user_id = legacy.user_id
   AND user_tag.category = 'SIDE_EFFECT'
   AND user_tag.name = legacy.side_effect
   AND user_tag.tag_type = 'NONE'
  WHERE legacy.user_id IS NOT NULL
) AS new_mappings
ON DUPLICATE KEY UPDATE
  user_id = new_mappings.user_id,
  journal_tag_id = new_mappings.journal_tag_id;

INSERT INTO legacy_journal_tag_mapping (legacy_category, legacy_tag_id, user_id, journal_tag_id)
SELECT legacy_category, legacy_tag_id, user_id, journal_tag_id
FROM (
  SELECT 'TROUBLE' AS legacy_category, legacy.id AS legacy_tag_id,
         legacy.user_id, COALESCE(system_tag.id, user_tag.id) AS journal_tag_id
  FROM trouble_tags legacy
  LEFT JOIN journal_tags system_tag
    ON system_tag.scope = 'SYSTEM'
   AND system_tag.category = 'TROUBLE'
   AND system_tag.name = legacy.trouble
   AND system_tag.tag_type = legacy.type
  LEFT JOIN journal_tags user_tag
    ON user_tag.scope = 'USER'
   AND user_tag.owner_user_id = legacy.user_id
   AND user_tag.category = 'TROUBLE'
   AND user_tag.name = legacy.trouble
   AND user_tag.tag_type = legacy.type
  WHERE legacy.user_id IS NOT NULL
) AS new_mappings
ON DUPLICATE KEY UPDATE
  user_id = new_mappings.user_id,
  journal_tag_id = new_mappings.journal_tag_id;

-- 5) Preserve per-user enabled/visible state. Duplicate legacy rows are merged with OR semantics.
INSERT INTO user_journal_tag_preferences (user_id, journal_tag_id, enabled, visible)
SELECT user_id, journal_tag_id, enabled, visible
FROM (
  SELECT legacy.user_id, mapping.journal_tag_id,
         MAX(legacy.is_active) AS enabled, MAX(legacy.visible) AS visible
  FROM condition_tags legacy
  JOIN legacy_journal_tag_mapping mapping
    ON mapping.legacy_category = 'CONDITION'
   AND mapping.legacy_tag_id = legacy.id
  WHERE legacy.user_id IS NOT NULL
  GROUP BY legacy.user_id, mapping.journal_tag_id
) AS new_preferences
ON DUPLICATE KEY UPDATE
  enabled = new_preferences.enabled,
  visible = new_preferences.visible;

INSERT INTO user_journal_tag_preferences (user_id, journal_tag_id, enabled, visible)
SELECT user_id, journal_tag_id, enabled, visible
FROM (
  SELECT legacy.user_id, mapping.journal_tag_id,
         MAX(legacy.is_active) AS enabled, MAX(legacy.visible) AS visible
  FROM side_effect_tags legacy
  JOIN legacy_journal_tag_mapping mapping
    ON mapping.legacy_category = 'SIDE_EFFECT'
   AND mapping.legacy_tag_id = legacy.id
  WHERE legacy.user_id IS NOT NULL
  GROUP BY legacy.user_id, mapping.journal_tag_id
) AS new_preferences
ON DUPLICATE KEY UPDATE
  enabled = new_preferences.enabled,
  visible = new_preferences.visible;

INSERT INTO user_journal_tag_preferences (user_id, journal_tag_id, enabled, visible)
SELECT user_id, journal_tag_id, enabled, visible
FROM (
  SELECT legacy.user_id, mapping.journal_tag_id,
         MAX(legacy.is_active) AS enabled, MAX(legacy.visible) AS visible
  FROM trouble_tags legacy
  JOIN legacy_journal_tag_mapping mapping
    ON mapping.legacy_category = 'TROUBLE'
   AND mapping.legacy_tag_id = legacy.id
  WHERE legacy.user_id IS NOT NULL
  GROUP BY legacy.user_id, mapping.journal_tag_id
) AS new_preferences
ON DUPLICATE KEY UPDATE
  enabled = new_preferences.enabled,
  visible = new_preferences.visible;

COMMIT;

-- 6) Backfill additive log columns. Existing legacy tag columns remain unchanged.
--    Runs outside the main transaction in 10,000-row batches to avoid long-held
--    table locks and replication lag on large log tables.
DROP PROCEDURE IF EXISTS _backfill_condition_logs;
DROP PROCEDURE IF EXISTS _backfill_side_effect_logs;
DROP PROCEDURE IF EXISTS _backfill_trouble_logs;

DELIMITER $$

CREATE PROCEDURE _backfill_condition_logs()
BEGIN
  REPEAT
    UPDATE condition_logs legacy_log
    JOIN legacy_journal_tag_mapping mapping
      ON mapping.legacy_category = 'CONDITION'
     AND mapping.legacy_tag_id = legacy_log.condition_tag_id
    SET legacy_log.user_id = mapping.user_id,
        legacy_log.journal_tag_id = mapping.journal_tag_id
    WHERE legacy_log.user_id IS NULL
       OR legacy_log.journal_tag_id IS NULL
    LIMIT 10000;
  UNTIL ROW_COUNT() = 0
  END REPEAT;
END$$

CREATE PROCEDURE _backfill_side_effect_logs()
BEGIN
  REPEAT
    UPDATE side_effect_logs legacy_log
    JOIN legacy_journal_tag_mapping mapping
      ON mapping.legacy_category = 'SIDE_EFFECT'
     AND mapping.legacy_tag_id = legacy_log.side_effect_tag_id
    SET legacy_log.user_id = mapping.user_id,
        legacy_log.journal_tag_id = mapping.journal_tag_id
    WHERE legacy_log.user_id IS NULL
       OR legacy_log.journal_tag_id IS NULL
    LIMIT 10000;
  UNTIL ROW_COUNT() = 0
  END REPEAT;
END$$

CREATE PROCEDURE _backfill_trouble_logs()
BEGIN
  REPEAT
    UPDATE trouble_logs legacy_log
    JOIN legacy_journal_tag_mapping mapping
      ON mapping.legacy_category = 'TROUBLE'
     AND mapping.legacy_tag_id = legacy_log.trouble_tag_id
    SET legacy_log.user_id = mapping.user_id,
        legacy_log.journal_tag_id = mapping.journal_tag_id
    WHERE legacy_log.user_id IS NULL
       OR legacy_log.journal_tag_id IS NULL
    LIMIT 10000;
  UNTIL ROW_COUNT() = 0
  END REPEAT;
END$$

DELIMITER ;

CALL _backfill_condition_logs();
CALL _backfill_side_effect_logs();
CALL _backfill_trouble_logs();

DROP PROCEDURE IF EXISTS _backfill_condition_logs;
DROP PROCEDURE IF EXISTS _backfill_side_effect_logs;
DROP PROCEDURE IF EXISTS _backfill_trouble_logs;

-- Verification: every legacy user tag and log must be mapped.
SELECT 'condition_tags' AS source, COUNT(*) AS unmapped_count
FROM condition_tags legacy
LEFT JOIN legacy_journal_tag_mapping mapping
  ON mapping.legacy_category = 'CONDITION' AND mapping.legacy_tag_id = legacy.id
WHERE legacy.user_id IS NOT NULL AND mapping.journal_tag_id IS NULL
UNION ALL
SELECT 'side_effect_tags', COUNT(*)
FROM side_effect_tags legacy
LEFT JOIN legacy_journal_tag_mapping mapping
  ON mapping.legacy_category = 'SIDE_EFFECT' AND mapping.legacy_tag_id = legacy.id
WHERE legacy.user_id IS NOT NULL AND mapping.journal_tag_id IS NULL
UNION ALL
SELECT 'trouble_tags', COUNT(*)
FROM trouble_tags legacy
LEFT JOIN legacy_journal_tag_mapping mapping
  ON mapping.legacy_category = 'TROUBLE' AND mapping.legacy_tag_id = legacy.id
WHERE legacy.user_id IS NOT NULL AND mapping.journal_tag_id IS NULL
UNION ALL
SELECT 'condition_logs', COUNT(*) FROM condition_logs WHERE user_id IS NULL OR journal_tag_id IS NULL
UNION ALL
SELECT 'side_effect_logs', COUNT(*) FROM side_effect_logs WHERE user_id IS NULL OR journal_tag_id IS NULL
UNION ALL
SELECT 'trouble_logs', COUNT(*) FROM trouble_logs WHERE user_id IS NULL OR journal_tag_id IS NULL;
