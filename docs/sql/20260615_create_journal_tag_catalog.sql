-- Additive journal tag catalog schema for production (MySQL 8.4)
-- Run once before deploying code that uses the new catalog.
-- This migration does not change existing reads or writes and does not backfill data.

CREATE TABLE IF NOT EXISTS journal_tags (
  id BIGINT NOT NULL AUTO_INCREMENT,
  category VARCHAR(30) NOT NULL,
  name VARCHAR(255) NOT NULL,
  tag_type VARCHAR(50) NOT NULL DEFAULT 'NONE',
  scope VARCHAR(20) NOT NULL,
  owner_user_id BINARY(16) NULL,
  owner_key BINARY(16) NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  default_visible BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_journal_tags_identity (scope, owner_key, category, name, tag_type),
  KEY idx_journal_tags_catalog_lookup (scope, category, is_active),
  KEY idx_journal_tags_owner_lookup (owner_user_id, category, is_active),
  CONSTRAINT fk_journal_tags_owner
    FOREIGN KEY (owner_user_id) REFERENCES users(id)
    ON DELETE CASCADE,
  CONSTRAINT ck_journal_tags_category
    CHECK (category IN ('CONDITION', 'SIDE_EFFECT', 'TROUBLE')),
  CONSTRAINT ck_journal_tags_scope_owner
    CHECK (
      (
        scope = 'SYSTEM'
        AND owner_user_id IS NULL
        AND owner_key = 0x00000000000000000000000000000000
      )
      OR (
        scope = 'USER'
        AND owner_user_id IS NOT NULL
        AND owner_key = owner_user_id
      )
    )
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_journal_tag_preferences (
  user_id BINARY(16) NOT NULL,
  journal_tag_id BIGINT NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  visible BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (user_id, journal_tag_id),
  KEY idx_user_journal_tag_preferences_tag (journal_tag_id),
  CONSTRAINT fk_user_journal_tag_preferences_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE,
  CONSTRAINT fk_user_journal_tag_preferences_tag
    FOREIGN KEY (journal_tag_id) REFERENCES journal_tags(id)
    ON DELETE CASCADE
) ENGINE=InnoDB;

-- Keeps the relationship between every legacy tag row and its catalog tag.
-- user_id is nullable because legacy default template rows have user_id = NULL.
CREATE TABLE IF NOT EXISTS legacy_journal_tag_mapping (
  legacy_category VARCHAR(30) NOT NULL,
  legacy_tag_id BIGINT NOT NULL,
  user_id BINARY(16) NULL,
  journal_tag_id BIGINT NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (legacy_category, legacy_tag_id),
  KEY idx_legacy_journal_tag_mapping_catalog (journal_tag_id),
  KEY idx_legacy_journal_tag_mapping_user (user_id),
  CONSTRAINT fk_legacy_journal_tag_mapping_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE,
  CONSTRAINT fk_legacy_journal_tag_mapping_catalog
    FOREIGN KEY (journal_tag_id) REFERENCES journal_tags(id)
    ON DELETE CASCADE,
  CONSTRAINT ck_legacy_journal_tag_mapping_category
    CHECK (legacy_category IN ('CONDITION', 'SIDE_EFFECT', 'TROUBLE'))
) ENGINE=InnoDB;

-- Add nullable columns only. Existing application behavior remains unchanged.
ALTER TABLE condition_logs
  ADD COLUMN user_id BINARY(16) NULL,
  ADD COLUMN journal_tag_id BIGINT NULL,
  ADD INDEX idx_condition_logs_user_checked_at (user_id, checked_at),
  ADD INDEX idx_condition_logs_journal_tag_checked_at (journal_tag_id, checked_at),
  ADD CONSTRAINT fk_condition_logs_catalog_tag
    FOREIGN KEY (journal_tag_id) REFERENCES journal_tags(id);

ALTER TABLE side_effect_logs
  ADD COLUMN user_id BINARY(16) NULL,
  ADD COLUMN journal_tag_id BIGINT NULL,
  ADD INDEX idx_side_effect_logs_user_checked_at (user_id, checked_at),
  ADD INDEX idx_side_effect_logs_journal_tag_checked_at (journal_tag_id, checked_at),
  ADD CONSTRAINT fk_side_effect_logs_catalog_tag
    FOREIGN KEY (journal_tag_id) REFERENCES journal_tags(id);

ALTER TABLE trouble_logs
  ADD COLUMN user_id BINARY(16) NULL,
  ADD COLUMN journal_tag_id BIGINT NULL,
  ADD INDEX idx_trouble_logs_user_checked_at (user_id, checked_at),
  ADD INDEX idx_trouble_logs_journal_tag_checked_at (journal_tag_id, checked_at),
  ADD CONSTRAINT fk_trouble_logs_catalog_tag
    FOREIGN KEY (journal_tag_id) REFERENCES journal_tags(id);

ALTER TABLE daily_goals
  ADD COLUMN saved_at DATETIME(6) NULL;
