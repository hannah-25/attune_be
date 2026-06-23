-- ============================================================
-- Journal Tag Direct Cutover — MySQL 8.4
-- docs/journal-tag-direct-cutover-plan.md 7장 Phase 1
--
-- 지원 baseline:
--   LEGACY_ONLY     : category별 태그·로그 테이블만 있는 초기 상태
--   ADDITIVE_CATALOG: 20260615_create_journal_tag_catalog.sql 적용 완료 상태
--
-- 실행 전 반드시 DB 스냅샷을 생성할 것.
-- MySQL DDL은 implicit commit이므로 전체를 하나의 트랜잭션으로 볼 수 없다.
-- postflight 통과 전까지 maintenance 상태를 유지할 것.
-- ============================================================

DELIMITER $$

DROP PROCEDURE IF EXISTS __attune_jt_cutover__ $$

CREATE PROCEDURE __attune_jt_cutover__()
proc_body: BEGIN

    DECLARE v_db            VARCHAR(64)  DEFAULT DATABASE();
    DECLARE v_baseline      VARCHAR(20)  DEFAULT NULL;
    DECLARE v_fk_name       VARCHAR(255) DEFAULT NULL;
    DECLARE v_count         INT          DEFAULT 0;

    -- ============================================================
    -- § 1  PREFLIGHT — baseline 감지
    -- ============================================================

    SELECT COUNT(*) INTO v_count
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = v_db AND TABLE_NAME = 'condition_tags';
    SET @has_legacy := v_count;

    SELECT COUNT(*) INTO v_count
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = v_db AND TABLE_NAME = 'journal_tags';
    SET @has_jt := v_count;

    SELECT COUNT(*) INTO v_count
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = v_db AND TABLE_NAME = 'legacy_journal_tag_mapping';
    SET @has_mapping := v_count;

    SELECT COUNT(*) INTO v_count
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = v_db AND TABLE_NAME = 'condition_logs' AND COLUMN_NAME = 'journal_tag_id';
    SET @has_catalog_cols := v_count;

    IF @has_legacy = 0 AND @has_jt = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '[PREFLIGHT FAIL] 레거시 태그 테이블이 없습니다. 빈 DB라면 앱을 직접 기동하십시오.';
    END IF;

    IF @has_legacy = 1 AND @has_jt = 0 AND @has_mapping = 0 AND @has_catalog_cols = 0 THEN
        SET v_baseline = 'LEGACY_ONLY';
    ELSEIF @has_legacy = 1 AND @has_jt = 1 AND @has_mapping = 1 AND @has_catalog_cols = 1 THEN
        SET v_baseline = 'ADDITIVE_CATALOG';
    ELSE
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '[PREFLIGHT FAIL] 인식할 수 없는 중간 상태입니다. 수동으로 스키마를 확인하십시오.';
    END IF;

    SELECT CONCAT('[PREFLIGHT OK] baseline = ', v_baseline) AS status;

    -- ============================================================
    -- § 2  SEED STAGING — 기본 시스템 태그 추출
    --       TEMPORARY TABLE이 아니어야 부분 실패 후에도 남는다.
    -- ============================================================

    DROP TABLE IF EXISTS cutover_jt_staging;
    CREATE TABLE cutover_jt_staging (
        category    VARCHAR(30)  NOT NULL,
        tag_name    VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_ci NOT NULL,
        tag_type    VARCHAR(50)  NOT NULL,
        def_visible BOOLEAN      NOT NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci;

    INSERT INTO cutover_jt_staging (category, tag_name, tag_type, def_visible)
    SELECT 'CONDITION',   condition_name, type,   visible FROM condition_tags   WHERE user_id IS NULL AND is_active = 1
    UNION ALL
    SELECT 'SIDE_EFFECT', side_effect,   'NONE',  visible FROM side_effect_tags WHERE user_id IS NULL AND is_active = 1
    UNION ALL
    SELECT 'TROUBLE',     trouble,        type,   visible FROM trouble_tags     WHERE user_id IS NULL AND is_active = 1;

    -- 중복 identity 체크
    SELECT COUNT(*) INTO v_count FROM (
        SELECT category, tag_name, tag_type
        FROM cutover_jt_staging
        GROUP BY category, tag_name, tag_type
        HAVING COUNT(*) > 1
    ) dups;

    IF v_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '[STAGING FAIL] 기본 태그에 중복 identity가 있습니다.';
    END IF;

    SELECT COUNT(*) INTO v_count FROM cutover_jt_staging;
    SELECT CONCAT('[STAGING OK] seed 행 수 = ', v_count) AS status;

    -- ============================================================
    -- § 3  DESTRUCTIVE DDL
    --       FK_CHECKS 비활성화로 삭제 순서 의존성 제거.
    --       MySQL DDL implicit commit 이후 복구는 스냅샷으로만 가능.
    -- ============================================================

    SET FOREIGN_KEY_CHECKS = 0;

    IF v_baseline = 'ADDITIVE_CATALOG' THEN
        DROP TABLE IF EXISTS legacy_journal_tag_mapping;
        DROP TABLE IF EXISTS user_journal_tag_preferences;
        DROP TABLE IF EXISTS journal_tags;
    END IF;

    DROP TABLE IF EXISTS condition_logs;
    DROP TABLE IF EXISTS side_effect_logs;
    DROP TABLE IF EXISTS trouble_logs;
    DROP TABLE IF EXISTS condition_tags;
    DROP TABLE IF EXISTS side_effect_tags;
    DROP TABLE IF EXISTS trouble_tags;

    SET FOREIGN_KEY_CHECKS = 1;

    SELECT '[DESTRUCTIVE DDL OK]' AS status;

    -- ============================================================
    -- § 4  TARGET DDL
    -- ============================================================

    CREATE TABLE journal_tags (
        id              BIGINT          NOT NULL AUTO_INCREMENT,
        category        VARCHAR(30)     NOT NULL,
        name            VARCHAR(255)
                        CHARACTER SET utf8mb4
                        COLLATE utf8mb4_0900_as_ci
                        NOT NULL,
        tag_type        VARCHAR(50)     NOT NULL,
        scope           VARCHAR(20)     NOT NULL,
        owner_user_id   BINARY(16)      NULL,
        owner_key       BINARY(16)      NOT NULL,
        is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
        default_visible BOOLEAN         NOT NULL DEFAULT FALSE,
        created_at      DATETIME(6)     NOT NULL,
        updated_at      DATETIME(6)     NOT NULL,
        PRIMARY KEY (id),
        UNIQUE KEY uk_journal_tags_identity (scope, owner_key, category, name, tag_type),
        KEY idx_journal_tags_catalog_lookup (scope, category, is_active),
        KEY idx_journal_tags_owner_lookup (owner_user_id, category, is_active),
        CONSTRAINT fk_journal_tags_owner
            FOREIGN KEY (owner_user_id) REFERENCES users(id)
            ON DELETE RESTRICT,
        CONSTRAINT ck_journal_tags_category
            CHECK (category IN ('CONDITION', 'SIDE_EFFECT', 'TROUBLE')),
        CONSTRAINT ck_journal_tags_scope
            CHECK (scope IN ('SYSTEM', 'USER')),
        CONSTRAINT ck_journal_tags_scope_owner
            CHECK (
                (scope = 'SYSTEM' AND owner_user_id IS NULL     AND owner_key = 0x00000000000000000000000000000000)
                OR
                (scope = 'USER'   AND owner_user_id IS NOT NULL AND owner_key = owner_user_id)
            )
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci;

    CREATE TABLE user_journal_tag_preferences (
        user_id         BINARY(16)  NOT NULL,
        journal_tag_id  BIGINT      NOT NULL,
        enabled         BOOLEAN     NOT NULL,
        visible         BOOLEAN     NOT NULL,
        created_at      DATETIME(6) NOT NULL,
        updated_at      DATETIME(6) NOT NULL,
        PRIMARY KEY (user_id, journal_tag_id),
        KEY idx_user_journal_tag_preferences_tag (journal_tag_id),
        CONSTRAINT fk_user_journal_tag_preferences_user
            FOREIGN KEY (user_id) REFERENCES users(id)
            ON DELETE CASCADE,
        CONSTRAINT fk_user_journal_tag_preferences_tag
            FOREIGN KEY (journal_tag_id) REFERENCES journal_tags(id)
            ON DELETE CASCADE,
        CONSTRAINT ck_user_journal_tag_preferences_visible
            CHECK (enabled = TRUE OR visible = FALSE)
    ) ENGINE=InnoDB;

    CREATE TABLE journal_tag_logs (
        id              BIGINT      NOT NULL AUTO_INCREMENT,
        user_id         BINARY(16)  NOT NULL,
        journal_tag_id  BIGINT      NOT NULL,
        journal_date    DATE        NOT NULL,
        checked_at      DATETIME(6) NOT NULL,
        PRIMARY KEY (id),
        UNIQUE KEY uk_journal_tag_logs_daily_check (user_id, journal_tag_id, journal_date),
        KEY idx_journal_tag_logs_user_date (user_id, journal_date),
        KEY idx_journal_tag_logs_tag_checked_at (journal_tag_id, checked_at),
        CONSTRAINT fk_journal_tag_logs_user
            FOREIGN KEY (user_id) REFERENCES users(id)
            ON DELETE CASCADE,
        CONSTRAINT fk_journal_tag_logs_tag
            FOREIGN KEY (journal_tag_id) REFERENCES journal_tags(id)
            ON DELETE RESTRICT
    ) ENGINE=InnoDB;

    SELECT '[TARGET DDL OK]' AS status;

    -- ============================================================
    -- § 5  SEED — staging에서 시스템 기본 태그 삽입
    -- ============================================================

    INSERT INTO journal_tags
        (category, name, tag_type, scope, owner_user_id, owner_key,
         is_active, default_visible, created_at, updated_at)
    SELECT
        category, tag_name, tag_type,
        'SYSTEM', NULL, 0x00000000000000000000000000000000,
        TRUE, def_visible, NOW(6), NOW(6)
    FROM cutover_jt_staging;

    SELECT COUNT(*) INTO v_count FROM journal_tags WHERE scope = 'SYSTEM';
    SELECT CONCAT('[SEED OK] SYSTEM 태그 수 = ', v_count) AS status;

    -- ============================================================
    -- § 6  POSTFLIGHT — 스키마·seed 검증 후 staging 제거
    -- ============================================================

    -- 레거시 테이블이 없는지
    SELECT COUNT(*) INTO v_count
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = v_db
      AND TABLE_NAME IN ('condition_tags', 'side_effect_tags', 'trouble_tags',
                         'condition_logs', 'side_effect_logs', 'trouble_logs',
                         'legacy_journal_tag_mapping');
    IF v_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '[POSTFLIGHT FAIL] 레거시 테이블이 남아 있습니다.';
    END IF;

    -- 새 테이블 3개 존재 확인
    SELECT COUNT(*) INTO v_count
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = v_db
      AND TABLE_NAME IN ('journal_tags', 'user_journal_tag_preferences', 'journal_tag_logs');
    IF v_count <> 3 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '[POSTFLIGHT FAIL] 신규 테이블 3개 중 일부가 없습니다.';
    END IF;

    -- journal_tags FK ON DELETE RESTRICT 확인
    SELECT COUNT(*) INTO v_count
    FROM information_schema.REFERENTIAL_CONSTRAINTS rc
    JOIN information_schema.KEY_COLUMN_USAGE kcu
        ON  rc.CONSTRAINT_NAME   = kcu.CONSTRAINT_NAME
        AND rc.CONSTRAINT_SCHEMA = kcu.TABLE_SCHEMA
        AND rc.TABLE_NAME        = kcu.TABLE_NAME
    WHERE rc.CONSTRAINT_SCHEMA = v_db
      AND rc.TABLE_NAME        = 'journal_tags'
      AND kcu.REFERENCED_TABLE_NAME = 'users'
      AND rc.DELETE_RULE        = 'RESTRICT';
    IF v_count = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '[POSTFLIGHT FAIL] journal_tags owner FK ON DELETE RESTRICT 없음.';
    END IF;

    -- user_journal_tag_preferences CHECK constraint 확인
    SELECT COUNT(*) INTO v_count
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA    = v_db
      AND TABLE_NAME      = 'user_journal_tag_preferences'
      AND CONSTRAINT_TYPE = 'CHECK';
    IF v_count = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '[POSTFLIGHT FAIL] user_journal_tag_preferences CHECK constraint 없음.';
    END IF;

    -- seed 수 검증
    SELECT COUNT(*) INTO @staging_cnt FROM cutover_jt_staging;
    SELECT COUNT(*) INTO @jt_system_cnt FROM journal_tags WHERE scope = 'SYSTEM';
    IF @staging_cnt <> @jt_system_cnt THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '[POSTFLIGHT FAIL] seed 행 수 불일치.';
    END IF;

    SELECT '[POSTFLIGHT OK]' AS status;

    -- staging 제거 (postflight 성공 후에만)
    DROP TABLE IF EXISTS cutover_jt_staging;

    SELECT '[DONE] journal tag cutover 완료' AS status;

END $$

DELIMITER ;

CALL __attune_jt_cutover__;
DROP PROCEDURE IF EXISTS __attune_jt_cutover__;
