-- ============================================================================
-- DB Indexing Effect Measurement - Seed Script
--
-- Target: dedicated measurement MySQL 8.4 instance only. NEVER run on dev/prod.
-- Plan:   docs/performance/db-indexing-effect-plan.md
--
-- Fully deterministic: every value is derived from row numbers via MD5, so
-- rebuilding the dataset for the after-measurement produces identical data.
-- No RAND(), no NOW().
--
-- Row counts produced:
--   users                     1,000  (user_status ACTIVE, deterministic UUIDs)
--   schedule_categories       1,000  (1 per user)
--   medications                  10
--   medication_dosages           30  (3 per medication)
--   schedules               100,000  (100 per user, ~20% external, ~5% deleted)
--   todos                   100,000  (100 per user)
--   user_medications          2,000  (2 per user; 1 active + 1 ended)
--   user_medication_schedules 4,000  (2 dose times per user medication)
--   user_medication_logs    120,000  (30 days per schedule)
--   daily_status_logs       120,000  (120 days per user)
--   memos                   120,000  (120 days per user)
--   journal_tags                 24  (system catalog)
--   journal_tag_logs        300,000  (100 days x 3 tags per user)
--   community_boards         50,000  (~5% deleted)
--   comments                202,000  (200k spread + 2k on hot post 123)
--
-- Measurement anchor values (see plan Step 2):
--   measured user : id = UNHEX(MD5('attune-user-500'))
--   hot post      : community_boards.id = 123
--   date anchor   : 2026-07-01 (data spans roughly the prior 2 years)
--
-- Usage (container name attune-perf-mysql, db attune_perf):
--   docker exec -i attune-perf-mysql mysql -uroot -p<pw> attune_perf < this-file.sql
-- ============================================================================

SET SESSION cte_max_recursion_depth = 400000;
SET SESSION sql_mode = 'STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

-- Clean re-run: truncate in FK-safe order.
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE user_medication_logs;
TRUNCATE TABLE user_medication_schedules;
TRUNCATE TABLE user_medications;
TRUNCATE TABLE medication_dosages;
TRUNCATE TABLE medications;
TRUNCATE TABLE comments;
TRUNCATE TABLE community_boards;
TRUNCATE TABLE journal_tag_logs;
TRUNCATE TABLE journal_tags;
TRUNCATE TABLE memos;
TRUNCATE TABLE daily_status_logs;
TRUNCATE TABLE schedules;
TRUNCATE TABLE schedule_categories;
TRUNCATE TABLE todos;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------------------------------------------------------
-- users: 1,000 ACTIVE users. id = UNHEX(MD5('attune-user-<n>')).
-- ----------------------------------------------------------------------------
INSERT INTO users
    (id, created_at, email, nickname, onboarding_skipped, user_status, user_type, password)
SELECT
    UNHEX(MD5(CONCAT('attune-user-', n))),
    TIMESTAMP('2024-07-01 09:00:00') + INTERVAL n MINUTE,
    CONCAT('seed-user-', n, '@example.com'),
    CONCAT('seed-user-', n),
    0,
    'ACTIVE',
    'USER',
    'seed-password-hash'
FROM (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 1000)
      SELECT n FROM seq) u;

-- ----------------------------------------------------------------------------
-- schedule_categories: 1 default category per user.
--   id = user_n, used by schedules below.
-- ----------------------------------------------------------------------------
INSERT INTO schedule_categories
    (id, user_id, category_name, color, is_active)
SELECT
    n,
    UNHEX(MD5(CONCAT('attune-user-', n))),
    'seed-default',
    '#4F46E5',
    1
FROM (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 1000)
      SELECT n FROM seq) c;

-- ----------------------------------------------------------------------------
-- schedules: 100 per user over ~2 years before 2026-07-01.
--   ~20% external (external_event_id set), ~5% soft-deleted.
--   start_time deterministic pseudo-random day/hour; duration 1h.
-- ----------------------------------------------------------------------------
INSERT INTO schedules
    (user_id, schedule_category_id, title, description, external_event_id, external_provider,
     place, is_all_day, is_deleted, alarm_enabled, start_time, end_time)
SELECT
    u.id,
    u.n,
    CONCAT('schedule-', u.n, '-', s.n),
    NULL,
    CASE WHEN MOD(CONV(SUBSTRING(MD5(CONCAT('sch-ext-', u.n, '-', s.n)), 1, 8), 16, 10), 10) < 2
         THEN CONCAT('ext-', u.n, '-', s.n) ELSE NULL END,
    CASE WHEN MOD(CONV(SUBSTRING(MD5(CONCAT('sch-ext-', u.n, '-', s.n)), 1, 8), 16, 10), 10) < 2
         THEN 'GOOGLE' ELSE NULL END,
    NULL,
    0,
    CASE WHEN MOD(CONV(SUBSTRING(MD5(CONCAT('sch-del-', u.n, '-', s.n)), 1, 8), 16, 10), 100) < 5
         THEN 1 ELSE 0 END,
    0,
    TIMESTAMP('2024-07-01 00:00:00')
        + INTERVAL MOD(CONV(SUBSTRING(MD5(CONCAT('sch-day-', u.n, '-', s.n)), 1, 8), 16, 10), 730) DAY
        + INTERVAL (8 + MOD(CONV(SUBSTRING(MD5(CONCAT('sch-hr-', u.n, '-', s.n)), 1, 8), 16, 10), 12)) HOUR,
    TIMESTAMP('2024-07-01 00:00:00')
        + INTERVAL MOD(CONV(SUBSTRING(MD5(CONCAT('sch-day-', u.n, '-', s.n)), 1, 8), 16, 10), 730) DAY
        + INTERVAL (9 + MOD(CONV(SUBSTRING(MD5(CONCAT('sch-hr-', u.n, '-', s.n)), 1, 8), 16, 10), 12)) HOUR
FROM (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 1000)
      SELECT n, UNHEX(MD5(CONCAT('attune-user-', n))) AS id FROM seq) u
JOIN (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 100)
      SELECT n FROM seq) s;

-- ----------------------------------------------------------------------------
-- todos: 100 per user over ~2 years. Past todos mostly completed/alarm-sent.
-- ----------------------------------------------------------------------------
INSERT INTO todos
    (user_id, text, due_at, is_all_day, is_completed, is_deleted, is_alarm_sent, created_at)
SELECT
    u.id,
    CONCAT('todo-', u.n, '-', s.n),
    TIMESTAMP('2024-07-01 00:00:00')
        + INTERVAL MOD(CONV(SUBSTRING(MD5(CONCAT('todo-day-', u.n, '-', s.n)), 1, 8), 16, 10), 730) DAY
        + INTERVAL (7 + MOD(CONV(SUBSTRING(MD5(CONCAT('todo-hr-', u.n, '-', s.n)), 1, 8), 16, 10), 14)) HOUR,
    CASE WHEN MOD(CONV(SUBSTRING(MD5(CONCAT('todo-ad-', u.n, '-', s.n)), 1, 8), 16, 10), 10) < 2 THEN 1 ELSE 0 END,
    CASE WHEN MOD(CONV(SUBSTRING(MD5(CONCAT('todo-cp-', u.n, '-', s.n)), 1, 8), 16, 10), 10) < 6 THEN 1 ELSE 0 END,
    CASE WHEN MOD(CONV(SUBSTRING(MD5(CONCAT('todo-dl-', u.n, '-', s.n)), 1, 8), 16, 10), 100) < 5 THEN 1 ELSE 0 END,
    CASE WHEN MOD(CONV(SUBSTRING(MD5(CONCAT('todo-as-', u.n, '-', s.n)), 1, 8), 16, 10), 10) < 8 THEN 1 ELSE 0 END,
    TIMESTAMP('2024-06-01 09:00:00') + INTERVAL u.n MINUTE
FROM (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 1000)
      SELECT n, UNHEX(MD5(CONCAT('attune-user-', n))) AS id FROM seq) u
JOIN (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 100)
      SELECT n FROM seq) s;

-- ----------------------------------------------------------------------------
-- medications catalog: 10 medications x 3 dosages.
-- ----------------------------------------------------------------------------
INSERT INTO medications (id, name, generic_name)
SELECT n, CONCAT('seed-medication-', n), CONCAT('generic-', n)
FROM (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 10)
      SELECT n FROM seq) m;

INSERT INTO medication_dosages (id, medication_id, amount, is_active)
SELECT (m.n - 1) * 3 + d.n, m.n, d.n * 10.0, 1
FROM (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 10)
      SELECT n FROM seq) m
JOIN (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 3)
      SELECT n FROM seq) d;

-- ----------------------------------------------------------------------------
-- user_medications: 2 per user.
--   k=1: active, started 2025-01-01+offset, no end date.
--   k=2: ended,   started 2024-07-01+offset, ended ~90 days later.
--   id = (user_n - 1) * 2 + k  (deterministic, used by schedules below)
-- ----------------------------------------------------------------------------
INSERT INTO user_medications
    (id, user_id, medication_dosage_id, consultation_id, is_active, alarm_active,
     started_at, end_at, created_at, updated_at)
SELECT
    (u.n - 1) * 2 + k.n,
    u.id,
    1 + MOD(u.n + k.n, 30),
    NULL,
    CASE WHEN k.n = 1 THEN 1 ELSE 0 END,
    1,
    CASE WHEN k.n = 1
         THEN DATE('2025-01-01') + INTERVAL MOD(u.n, 180) DAY
         ELSE DATE('2024-07-01') + INTERVAL MOD(u.n, 90) DAY END,
    CASE WHEN k.n = 1
         THEN NULL
         ELSE DATE('2024-07-01') + INTERVAL (MOD(u.n, 90) + 90) DAY END,
    TIMESTAMP('2024-07-01 10:00:00') + INTERVAL (u.n * 2 + k.n) MINUTE,
    TIMESTAMP('2024-07-01 10:00:00') + INTERVAL (u.n * 2 + k.n) MINUTE
FROM (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 1000)
      SELECT n, UNHEX(MD5(CONCAT('attune-user-', n))) AS id FROM seq) u
JOIN (SELECT 1 AS n UNION ALL SELECT 2) k;

-- ----------------------------------------------------------------------------
-- user_medication_schedules: 2 per user_medication (08:00, 20:00).
--   id = (user_medication_id - 1) * 2 + t  (deterministic, used by logs below)
-- ----------------------------------------------------------------------------
INSERT INTO user_medication_schedules
    (id, user_medication_id, dose_time, label, is_active)
SELECT
    (um.n - 1) * 2 + t.n,
    um.n,
    CASE WHEN t.n = 1 THEN TIME('08:00:00') ELSE TIME('20:00:00') END,
    CASE WHEN t.n = 1 THEN 'morning' ELSE 'evening' END,
    1
FROM (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 2000)
      SELECT n FROM seq) um
JOIN (SELECT 1 AS n UNION ALL SELECT 2) t;

-- ----------------------------------------------------------------------------
-- user_medication_logs: 30 daily logs per schedule (4,000 x 30 = 120,000).
--   taken_at = day + dose time (unique per schedule+time). ~85% TAKEN.
--   Window: 2026-05-01 .. 2026-05-30 + per-schedule day offset.
-- ----------------------------------------------------------------------------
INSERT INTO user_medication_logs
    (user_medication_schedule_id, taken_at, status, is_active)
SELECT
    s.n,
    TIMESTAMP('2026-04-01 00:00:00')
        + INTERVAL (MOD(s.n, 30) + d.n) DAY
        + INTERVAL CASE WHEN MOD(s.n, 2) = 1 THEN 8 ELSE 20 END HOUR
        + INTERVAL MOD(CONV(SUBSTRING(MD5(CONCAT('log-min-', s.n, '-', d.n)), 1, 8), 16, 10), 30) MINUTE,
    CASE WHEN MOD(CONV(SUBSTRING(MD5(CONCAT('log-st-', s.n, '-', d.n)), 1, 8), 16, 10), 100) < 85
         THEN 'TAKEN' ELSE 'SKIPPED' END,
    CASE WHEN MOD(CONV(SUBSTRING(MD5(CONCAT('log-ac-', s.n, '-', d.n)), 1, 8), 16, 10), 100) < 95
         THEN 1 ELSE 0 END
FROM (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 4000)
      SELECT n FROM seq) s
JOIN (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 30)
      SELECT n FROM seq) d;

-- ----------------------------------------------------------------------------
-- daily_status_logs: 120 consecutive days per user ending 2026-06-30.
-- ----------------------------------------------------------------------------
INSERT INTO daily_status_logs
    (user_id, date, sleep_hour, sleep_quality, ate_breakfast, ate_lunch, ate_dinner)
SELECT
    u.id,
    DATE('2026-06-30') - INTERVAL (d.n - 1) DAY,
    4 + MOD(CONV(SUBSTRING(MD5(CONCAT('dsl-sl-', u.n, '-', d.n)), 1, 8), 16, 10), 6),
    ELT(1 + MOD(CONV(SUBSTRING(MD5(CONCAT('dsl-q-', u.n, '-', d.n)), 1, 8), 16, 10), 3), 'BAD', 'NORMAL', 'GOOD'),
    MOD(CONV(SUBSTRING(MD5(CONCAT('dsl-b-', u.n, '-', d.n)), 1, 8), 16, 10), 2),
    MOD(CONV(SUBSTRING(MD5(CONCAT('dsl-l-', u.n, '-', d.n)), 1, 8), 16, 10), 2),
    MOD(CONV(SUBSTRING(MD5(CONCAT('dsl-d-', u.n, '-', d.n)), 1, 8), 16, 10), 2)
FROM (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 1000)
      SELECT n, UNHEX(MD5(CONCAT('attune-user-', n))) AS id FROM seq) u
JOIN (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 120)
      SELECT n FROM seq) d;

-- ----------------------------------------------------------------------------
-- memos: 120 consecutive days per user ending 2026-06-30.
-- ----------------------------------------------------------------------------
INSERT INTO memos (user_id, journal_date, memo)
SELECT
    u.id,
    DATE('2026-06-30') - INTERVAL (d.n - 1) DAY,
    CONCAT('memo-', u.n, '-', d.n)
FROM (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 1000)
      SELECT n, UNHEX(MD5(CONCAT('attune-user-', n))) AS id FROM seq) u
JOIN (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 120)
      SELECT n FROM seq) d;

-- ----------------------------------------------------------------------------
-- journal_tags: 24 system tags (8 per category).
--   owner_key = SYSTEM sentinel 0x00000000000000000000000000000000.
-- ----------------------------------------------------------------------------
INSERT INTO journal_tags
    (id, category, name, tag_type, scope, owner_user_id, owner_key,
     is_active, default_visible, created_at, updated_at)
SELECT
    n,
    ELT(1 + MOD(n - 1, 3), 'CONDITION', 'SIDE_EFFECT', 'TROUBLE'),
    CONCAT('seed-tag-', n),
    'NONE',
    'SYSTEM',
    NULL,
    UNHEX(REPEAT('0', 32)),
    1,
    1,
    TIMESTAMP('2024-07-01 00:00:00'),
    TIMESTAMP('2024-07-01 00:00:00')
FROM (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 24)
      SELECT n FROM seq) t;

-- ----------------------------------------------------------------------------
-- journal_tag_logs: 100 consecutive days x 3 distinct tags per user
--   ending 2026-06-30 (1,000 x 100 x 3 = 300,000).
-- ----------------------------------------------------------------------------
INSERT INTO journal_tag_logs (user_id, journal_tag_id, journal_date, checked_at)
SELECT
    u.id,
    1 + MOD(u.n + d.n * 3 + t.n * 7, 24),
    DATE('2026-06-30') - INTERVAL (d.n - 1) DAY,
    TIMESTAMP(DATE('2026-06-30') - INTERVAL (d.n - 1) DAY)
        + INTERVAL 21 HOUR + INTERVAL t.n MINUTE
FROM (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 1000)
      SELECT n, UNHEX(MD5(CONCAT('attune-user-', n))) AS id FROM seq) u
JOIN (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 100)
      SELECT n FROM seq) d
JOIN (SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3) t;

-- ----------------------------------------------------------------------------
-- community_boards: 50,000 posts over ~2 years, ~5% deleted.
-- ----------------------------------------------------------------------------
INSERT INTO community_boards
    (id, user_id, post_category, title, content, is_anonymous, is_deleted, created_at, updated_at)
SELECT
    p.n,
    UNHEX(MD5(CONCAT('attune-user-', 1 + MOD(p.n, 1000)))),
    ELT(1 + MOD(CONV(SUBSTRING(MD5(CONCAT('post-cat-', p.n)), 1, 8), 16, 10), 4),
        'DAILY_LIFE', 'DEFAULT', 'DISORDER_INFO', 'MEDICATION'),
    CONCAT('seed post title ', p.n),
    CONCAT('seed post content ', p.n),
    MOD(p.n, 5) = 0,
    CASE WHEN MOD(CONV(SUBSTRING(MD5(CONCAT('post-del-', p.n)), 1, 8), 16, 10), 100) < 5
         THEN 1 ELSE 0 END,
    TIMESTAMP('2024-07-01 00:00:00')
        + INTERVAL MOD(CONV(SUBSTRING(MD5(CONCAT('post-day-', p.n)), 1, 8), 16, 10), 730) DAY
        + INTERVAL MOD(CONV(SUBSTRING(MD5(CONCAT('post-min-', p.n)), 1, 8), 16, 10), 1440) MINUTE,
    NULL
FROM (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 50000)
      SELECT n FROM seq) p;

-- ----------------------------------------------------------------------------
-- comments: 200,000 spread (4 per post) + 2,000 on hot post 123.
-- ----------------------------------------------------------------------------
INSERT INTO comments
    (user_id, post_id, content, is_anonymous, is_deleted, created_at, updated_at)
SELECT
    UNHEX(MD5(CONCAT('attune-user-', 1 + MOD(c.n * 7, 1000)))),
    1 + MOD(CONV(SUBSTRING(MD5(CONCAT('cmt-post-', c.n)), 1, 8), 16, 10), 50000),
    CONCAT('seed comment ', c.n),
    MOD(c.n, 7) = 0,
    CASE WHEN MOD(CONV(SUBSTRING(MD5(CONCAT('cmt-del-', c.n)), 1, 8), 16, 10), 100) < 5
         THEN 1 ELSE 0 END,
    TIMESTAMP('2024-07-01 00:00:00')
        + INTERVAL MOD(CONV(SUBSTRING(MD5(CONCAT('cmt-day-', c.n)), 1, 8), 16, 10), 730) DAY
        + INTERVAL MOD(CONV(SUBSTRING(MD5(CONCAT('cmt-min-', c.n)), 1, 8), 16, 10), 1440) MINUTE,
    NULL
FROM (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 200000)
      SELECT n FROM seq) c;

INSERT INTO comments
    (user_id, post_id, content, is_anonymous, is_deleted, created_at, updated_at)
SELECT
    UNHEX(MD5(CONCAT('attune-user-', 1 + MOD(h.n * 3, 1000)))),
    123,
    CONCAT('seed hot comment ', h.n),
    0,
    CASE WHEN MOD(h.n, 20) = 0 THEN 1 ELSE 0 END,
    TIMESTAMP('2026-01-01 00:00:00') + INTERVAL h.n MINUTE,
    NULL
FROM (WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 2000)
      SELECT n FROM seq) h;

-- ----------------------------------------------------------------------------
-- Verification: row counts per table (record these in the result document).
-- ----------------------------------------------------------------------------
SELECT 'users' AS tbl, COUNT(*) AS cnt FROM users
UNION ALL SELECT 'schedule_categories', COUNT(*) FROM schedule_categories
UNION ALL SELECT 'schedules', COUNT(*) FROM schedules
UNION ALL SELECT 'todos', COUNT(*) FROM todos
UNION ALL SELECT 'medications', COUNT(*) FROM medications
UNION ALL SELECT 'medication_dosages', COUNT(*) FROM medication_dosages
UNION ALL SELECT 'user_medications', COUNT(*) FROM user_medications
UNION ALL SELECT 'user_medication_schedules', COUNT(*) FROM user_medication_schedules
UNION ALL SELECT 'user_medication_logs', COUNT(*) FROM user_medication_logs
UNION ALL SELECT 'daily_status_logs', COUNT(*) FROM daily_status_logs
UNION ALL SELECT 'memos', COUNT(*) FROM memos
UNION ALL SELECT 'journal_tags', COUNT(*) FROM journal_tags
UNION ALL SELECT 'journal_tag_logs', COUNT(*) FROM journal_tag_logs
UNION ALL SELECT 'community_boards', COUNT(*) FROM community_boards
UNION ALL SELECT 'comments', COUNT(*) FROM comments;
