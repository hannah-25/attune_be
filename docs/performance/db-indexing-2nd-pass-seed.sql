-- DB Indexing 2nd Pass - Candidate Table Seed Script
--
-- Target: dedicated measurement MySQL instance only. NEVER run on dev/prod.
-- Base prerequisite: docs/performance/db-indexing-effect-seed.sql has already populated users.
--
-- Row counts produced:
--   medication_analysis_reports 24,000  (24 per user)
--   admin_audit_logs           100,000
--   consultations              100,000  (100 per user, ~5% deleted)
--   onboarding_symptoms         10,000  (10 per user)
--   asrs_assessments            10,000  (10 per user)
--   user_settings                1,000  (1 per user)

SET SESSION cte_max_recursion_depth = 200000;
SET SESSION sql_mode = 'STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE user_settings;
TRUNCATE TABLE asrs_answers;
TRUNCATE TABLE asrs_assessments;
TRUNCATE TABLE onboarding_symptoms;
TRUNCATE TABLE consultations;
TRUNCATE TABLE admin_audit_logs;
TRUNCATE TABLE medication_analysis_reports;
SET FOREIGN_KEY_CHECKS = 1;

-- medication_analysis_reports: 24 reports per user.
INSERT INTO medication_analysis_reports
    (ai_result_json, generated_at, model_name, period_end, period_start, prompt_version,
     row_count_hash, snapshot_json, source_data_hash, status, user_id)
SELECT
    JSON_OBJECT('summary', CONCAT('seed report ', u.n, '-', r.n)),
    TIMESTAMP('2026-01-01 09:00:00') + INTERVAL r.n DAY + INTERVAL u.n MINUTE,
    'seed-model',
    DATE('2026-01-01') + INTERVAL r.n DAY,
    DATE('2026-01-01') + INTERVAL (r.n - 6) DAY,
    'v1',
    MD5(CONCAT('row-count-', u.n, '-', r.n)),
    JSON_OBJECT('snapshot', CONCAT('seed snapshot ', u.n, '-', r.n)),
    MD5(CONCAT('source-', u.n, '-', r.n)),
    CASE
        WHEN r.n % 11 = 0 THEN 'OUTDATED'
        WHEN r.n % 7 = 0 THEN 'PENDING'
        ELSE 'COMPLETED'
    END,
    UNHEX(MD5(CONCAT('attune-user-', u.n)))
FROM (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 1000)
      SELECT n FROM seq) u
CROSS JOIN (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 24)
            SELECT n FROM seq) r;

-- admin_audit_logs: latest-first audit feed candidate.
INSERT INTO admin_audit_logs
    (id, action, admin_email, admin_id, created_at, reason, target_label, target_reference)
SELECT
    UNHEX(MD5(CONCAT('audit-log-', n))),
    CASE n % 4
        WHEN 0 THEN 'MEMBER_DELETED'
        WHEN 1 THEN 'MEMBER_SOFT_DELETED'
        WHEN 2 THEN 'STATUS_CHANGED'
        ELSE 'WITHDRAWAL_CANCELLED'
    END,
    CONCAT('admin-', 1 + (n % 20), '@example.com'),
    UNHEX(MD5(CONCAT('admin-user-', 1 + (n % 20)))),
    TIMESTAMP('2026-01-01 00:00:00') + INTERVAL n MINUTE,
    CONCAT('seed audit reason ', n),
    CONCAT('member-', 1 + (n % 1000)),
    CONCAT('member_', 1 + (n % 1000))
FROM (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 100000)
      SELECT n FROM seq) s;

-- consultations: user-scoped date range candidate.
INSERT INTO consultations
    (alarm_settings, consultation_date, created_at, doctor_name, is_deleted, is_first_visit,
     place, updated_at, user_id)
SELECT
    b'1',
    TIMESTAMP('2025-01-01 10:00:00') + INTERVAL c.n DAY + INTERVAL u.n MINUTE,
    TIMESTAMP('2025-01-01 09:00:00') + INTERVAL c.n DAY,
    CONCAT('Doctor ', 1 + (u.n % 30)),
    IF(c.n % 20 = 0, b'1', b'0'),
    IF(c.n = 1, b'1', b'0'),
    CONCAT('Clinic ', 1 + (u.n % 50)),
    TIMESTAMP('2025-01-01 09:30:00') + INTERVAL c.n DAY,
    UNHEX(MD5(CONCAT('attune-user-', u.n)))
FROM (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 1000)
      SELECT n FROM seq) u
CROSS JOIN (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 100)
            SELECT n FROM seq) c;

-- onboarding_symptoms: latest symptom per user and latest-before-assessment candidate.
INSERT INTO onboarding_symptoms
    (description, emotional_event, is_quick_onboarding, saved_at,
     selected_functional_areas, selected_symptom_types, user_id)
SELECT
    CONCAT('seed symptom ', u.n, '-', s.n),
    CONCAT('seed event ', s.n),
    IF(s.n % 2 = 0, b'1', b'0'),
    TIMESTAMP('2025-06-01 08:00:00') + INTERVAL s.n DAY + INTERVAL u.n MINUTE,
    'WORK_STUDY,TIME_MANAGEMENT',
    'INATTENTION,TIME_MANAGEMENT',
    UNHEX(MD5(CONCAT('attune-user-', u.n)))
FROM (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 1000)
      SELECT n FROM seq) u
CROSS JOIN (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 10)
            SELECT n FROM seq) s;

-- asrs_assessments: latest/all/next assessment per user candidates.
INSERT INTO asrs_assessments
    (completed_at, partascore, total_score, user_id)
SELECT
    TIMESTAMP('2025-06-01 12:00:00') + INTERVAL a.n DAY + INTERVAL u.n MINUTE,
    a.n % 24,
    18 + (a.n % 54),
    UNHEX(MD5(CONCAT('attune-user-', u.n)))
FROM (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 1000)
      SELECT n FROM seq) u
CROSS JOIN (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 10)
            SELECT n FROM seq) a;

-- user_settings: notification batch candidate.
INSERT INTO user_settings
    (user_id, community_notification, marketing_notification, medication_notification,
     report_notification, take_medication_on_holiday, theme, timezone, todo_notification)
SELECT
    id,
    b'1',
    IF(ORD(SUBSTR(id, 16, 1)) % 4 = 0, b'1', b'0'),
    b'1',
    IF(ORD(SUBSTR(id, 15, 1)) % 3 = 0, b'1', b'0'),
    b'0',
    'SYSTEM',
    'Asia/Seoul',
    b'1'
FROM users;
