# DB Indexing Effect Measurement Plan

## Goal

DB indexing work must prove that each new index improves the target query before it is kept.
This plan measures indexing effect only. It does not run load tests and does not try to measure
maximum throughput under concurrent traffic.

## Scope

In scope:

- Compare query execution plans before and after index creation.
- Compare query execution time with the same SQL, parameters, and dataset.
- Check whether MySQL uses the expected index.
- Record whether rows scanned, filesort, or temporary table usage decreases.
- Keep only indexes with clear evidence.

Out of scope:

- Concurrent user load testing.
- API throughput testing.
- Long-running stress tests.
- Production traffic experiments.

## Target Queries

Start with the Repository methods below because they use user-scoped range queries, paging, or
large table scans.

| Area | Repository method | Primary access pattern |
|---|---|---|
| Schedule | `ScheduleRepository.findAllInRange` | `user_id`, `is_deleted`, `start_time`, `end_time`, optional `external_event_id` predicate |
| Todo | `TodoRepository.findAllByDate` | `user_id`, `is_deleted`, `due_at` |
| Todo alarm | `TodoRepository.findAlarmCandidates` | Existing `idx_todos_alarm_lookup` plus `users.user_status` join; verify only |
| Medication | `UserMedicationRepository.findAllByUserIdWithDetails` | `user_id`, `is_active`, `created_at`, `id` |
| Medication | `UserMedicationRepository.findAllOverlappingPeriod` | `user_id`, `started_at`, `end_at` |
| Medication log | `UserMedicationLogRepository.findAllByUserIdAndTakenAtBetween` | Existing `UNIQUE(user_medication_schedule_id, taken_at)` likely covers the log side; verify only. Driving side is `user_medications.user_id` (FK index) |
| Medication log | `UserMedicationLogRepository.countByUserIdAndTakenAtBetween` | Same as above; verify only |
| Journal | `DailyStatusLogRepository.findByUserIdAndDateBetween` | Existing `UNIQUE(user_id, date)` covers equality + range exactly; verify only |
| Journal | `MemoRepository.findByUserIdAndJournalDateBetween` | Existing `UNIQUE(user_id, journal_date)` covers equality + range exactly; verify only |
| Journal | `JournalTagLogRepository.findAllWithTagByUserIdAndJournalDateBetween` | `user_id`, `journal_date`, `checked_at`. If the DB has `UNIQUE(user_id, journal_tag_id, journal_date)`, it only helps on the `user_id` prefix because `journal_date` is the third column |
| Community | `CommunityBoardRepository.findAllByIsDeletedFalseOrderByCreatedAtDesc` | `is_deleted`, `created_at` |
| Community | `CommunityBoardRepository.searchPosts` | `is_deleted`, optional `post_category`, `created_at` |
| Community | `CommentRepository.findAllByCommunityBoardIdAndIsDeletedFalseOrderByCreatedAtAsc` | DB columns: `post_id`, `is_deleted`, `created_at` |

Do not add indexes for every method blindly. If `SHOW INDEX` shows an existing unique key or
foreign-key-backed index already covers the pattern, record that and skip the candidate.
Rows marked "verify only" are expected to be covered already; measure them to confirm, but do not
draft new indexes for them unless the baseline disproves the assumption.

### Per-query measurement notes

- `ScheduleRepository.findAllInRange` has a dynamic `:manualOnly IS NULL OR ...` OR-condition on
  `external_event_id`. The plan differs per binding, so measure all three cases (`NULL`, `true`,
  `false`) separately. Also, `start_time < :end AND end_time >= :start` is a double range: a B-tree
  index can use only one range column, so expect `range` access on one column plus a filter, not a
  fully covered scan.
- `CommunityBoardRepository.searchPosts` wraps columns in `LOWER(title) LIKE :q` /
  `LOWER(content) LIKE :q`. The function call alone defeats a plain B-tree index regardless of the
  wildcard position. The realistic index win here is the category-only path
  (`is_deleted`, `post_category`, `created_at`); keyword search improvement is out of scope.
- The two `Pageable` community queries execute a separate count query. Capture both `EXPLAIN` and
  `EXPLAIN ANALYZE` for the count query as well, not just the page query.
- `TodoRepository.findAlarmCandidates` joins `users` on `user_status = 'ACTIVE'`. Verify the
  `users` access path too, not only `idx_todos_alarm_lookup`.
- Verify whether `comments.post_id` already has an FK-backed index with `SHOW INDEX FROM comments`.
  If a composite index such as `(post_id, is_deleted, created_at)` is added, MySQL can use the
  leftmost `post_id` prefix for FK checks. Record any redundant single-column `post_id` index as a
  separate drop candidate instead of dropping it as part of the first performance pass.

## Measurement Environment And Dataset

Decide and record these before any measurement. Without them, `EXPLAIN ANALYZE` on a near-empty
table proves nothing: the optimizer will full-scan a tiny table in 0 ms whether or not an index
exists.

- Measurement DB: a dedicated local MySQL instance (for example a Docker container), never prod.
  Record the exact MySQL version. `EXPLAIN ANALYZE` requires MySQL 8.0.18 or later.
- Schema control: local/dev profiles use `ddl-auto: update`. For baseline measurement, do not run
  application code that includes new entity `@Table(indexes = ...)` declarations before the baseline
  is captured. Control the before/after schema change with the candidate SQL only, or the baseline
  may already include the index being measured.
- Data volume: seed each candidate table to a realistic size before the baseline. Minimum targets:
  - High-volume date/range tables (`user_medication_logs`, `daily_status_logs`, `journal_tag_logs`, `todos`,
    `schedules`): 100,000+ rows spread across 1,000+ users so that one user's range is a small
    fraction of the table.
  - Board tables (`community_boards`, `comments`): 50,000+ posts, 200,000+ comments.
  - Parent tables (`user_medications`, `user_medication_schedules`, `memos`): proportional to the
    log tables they join.
- Seed method: a repeatable script checked into the measurement branch (SQL or a small generator),
  so the exact same dataset can be rebuilt for the after-measurement. Record the seed row counts in
  the result document.

## Measurement Rules

Use the same environment for before and after measurements.

- Same DB instance.
- Same dataset.
- Same SQL.
- Same bind parameter values.
- Same connection/session settings where practical.
- Run each query more than once and ignore the first run if it is clearly a cache warmup outlier.

For each query, capture both plan outputs. They provide different fields: tabular `EXPLAIN` has the
`access_type` / `key` / `Extra` columns, while `EXPLAIN ANALYZE` prints a TREE-format iterator plan
with actual times and rows but no `Extra` column.

From tabular `EXPLAIN`:

- Selected access type: `ALL`, `range`, `ref`, `eq_ref`, etc.
- Chosen key.
- Estimated rows.
- `Extra` flags such as `Using where`, `Using index`, `Using filesort`, `Using temporary`.

From `EXPLAIN ANALYZE`:

- Actual rows.
- Actual execution time.
- Presence of `Sort:` / `Temporary table` nodes in the tree (cross-check against `Extra`).

## Step 1. Inventory Current Indexes

Run `SHOW INDEX` for each candidate table and paste the result summary into the measurement
document.

Candidate tables:

- `schedules`
- `todos`
- `user_medications`
- `user_medication_schedules`
- `user_medication_logs`
- `daily_status_logs`
- `memos`
- `journal_tag_logs`
- `community_boards`
- `comments`

Record existing unique keys separately because MySQL can use them as indexes.

## Step 2. Capture Baseline

For each target query:

1. Capture the actual SQL Hibernate generates instead of hand-translating the JPQL. Use the existing
   profile switches, for example set `LOGGING_LEVEL_HIBERNATE_SQL=DEBUG` and
   `LOGGING_LEVEL_HIBERNATE_BIND=TRACE`, run the endpoint or a repository test once, and copy the
   generated SQL with bind values. Hand-written equivalents can drift in join order or predicate
   shape and then measure the wrong plan.
2. Choose realistic parameters.
3. Run tabular `EXPLAIN` and `EXPLAIN ANALYZE`.
4. Record the baseline result.

Template:

```text
Target:
Repository:
SQL:
Parameters:

Before:
  access_type:
  key:
  estimated_rows:
  actual_rows:
  actual_time:
  extra:
  notes:
```

## Step 3. Create Candidate Indexes

Create a draft SQL file under `docs/sql/`, for example:

```text
docs/sql/YYYYMMDD_add_performance_indexes.sql
```

Rules:

- Prefer composite indexes that match equality predicates first, then range predicates, then
  ordering columns.
- Do not duplicate existing unique indexes.
- Do not add indexes for low-selectivity boolean columns alone.
- Do not add fulltext indexes as part of this pass unless the task explicitly expands to search
  performance. `LIKE '%keyword%'` is not solved by ordinary B-tree indexes.
- Keep each index tied to one or more measured queries.

## Step 4. Measure After Index Creation

Apply candidate indexes only to the measurement DB, then rerun the same `EXPLAIN` and
`EXPLAIN ANALYZE` commands.

Template:

```text
After:
  access_type:
  key:
  estimated_rows:
  actual_rows:
  actual_time:
  extra:
  notes:

Decision:
  keep | remove | defer
Reason:
```

## Keep Or Remove Criteria

Keep an index when at least one of these is true:

- Access changes from `ALL` scan to `range`, `ref`, or better.
- Actual scanned rows decrease substantially.
- Actual execution time decreases meaningfully for a realistic parameter set.
- `Using filesort` or `Using temporary` is removed from a hot query.
- The index supports a scheduler or batch query that runs frequently.

Remove or defer an index when any of these is true:

- MySQL does not choose it.
- It duplicates an existing unique or foreign-key index.
- Improvement is negligible.
- The table is write-heavy and read improvement is not clear.
- The query is better fixed by query shape, pagination, or fulltext search instead of a B-tree index.

## Result Document

Create a result document after measurement:

```text
docs/performance/db-indexing-effect-YYYYMMDD.md
```

Recommended summary table:

| Query | Before key | After key | Before time | After time | Before rows | After rows | Decision |
|---|---|---|---:|---:|---:|---:|---|
| Schedule range | `NULL` | `idx_schedules_user_deleted_start_end` | 0 ms | 0 ms | 0 | 0 | keep |

Use real measured values. Do not estimate improvement without measurement.

## Final Deliverables

When the measurement supports keeping one or more indexes, commit these together:

- `docs/sql/YYYYMMDD_add_performance_indexes.sql` - the authoritative script for prod, which runs
  `ddl-auto: validate` and never auto-creates indexes. Prod apply is manual execution of this file.
- Entity `@Table(indexes = ...)` updates - dev and local run `ddl-auto: update`, so these
  declarations actually create the indexes there. Keep entity declarations and the SQL file in sync.
- `docs/db_schema.md` updates for affected tables. If this work touches `memos`, add the missing
  `memos` section. If it touches `user_medication_logs`, correct the documented `takenAt` column
  name to the actual DB column `taken_at`.
- `docs/performance/db-indexing-effect-YYYYMMDD.md` with before/after evidence.

Run at least:

```powershell
.\gradlew.bat --no-daemon test
```

If entity mappings or schema-related code changed, prefer:

```powershell
.\gradlew.bat --no-daemon build
```
