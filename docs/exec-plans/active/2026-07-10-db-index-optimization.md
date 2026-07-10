# DB Index Optimization Plan

- Status: active
- Owner / Date: Codex / 2026-07-10
- Related docs:
  - `docs/performance/db-indexing-effect-plan.md`
  - `docs/performance/db-indexing-effect-20260706.md`
  - `docs/sql/20260706_add_performance_indexes.sql`
  - `docs/db_schema.md`
  - `docs/architecture/data-rules.md`

## Goal

Improve database read performance with measured, query-shaped B-tree indexes only where they are
useful. Avoid adding indexes that duplicate existing primary, unique, foreign-key, or previously
verified performance indexes.

## Background

MySQL/InnoDB already uses B-tree indexes for primary keys, unique keys, and ordinary secondary
indexes. The useful work is therefore not "turn on B-tree indexing"; it is choosing composite
indexes that match Attune's hot query predicates and sort order.

The project already has a completed first performance-indexing pass:

- Measurement plan: `docs/performance/db-indexing-effect-plan.md`
- Measurement result: `docs/performance/db-indexing-effect-20260706.md`
- Production SQL: `docs/sql/20260706_add_performance_indexes.sql`

This plan continues from that baseline.

## Scope

In scope:

- Inventory current code-declared indexes, unique constraints, and performance SQL indexes.
- Map current repository query patterns to existing coverage.
- Identify remaining index candidates with enough expected value to measure.
- Measure candidates with `SHOW INDEX`, `EXPLAIN`, and `EXPLAIN ANALYZE` before keeping them.
- If measurement supports changes, update entity `@Table(indexes = ...)`, `docs/sql/`, and
  `docs/db_schema.md` together.

Out of scope:

- Full-text search optimization for `LIKE '%keyword%'`.
- Production traffic experiments.
- Adding indexes only because a column appears in a query.
- Dropping existing indexes without separate proof and rollout planning.

## Current Schema Control

- Flyway/Liquibase is not used.
- `local` and `dev` profiles use Hibernate `ddl-auto: update`.
- `prod` uses Hibernate `ddl-auto: validate`.
- Production schema changes must be represented as manual SQL in `docs/sql/` before deployment.

## Step 1. Current Index Inventory

Status: completed from code and docs on 2026-07-10. Actual DB verification still requires
`SHOW INDEX` against the target database.

### Previously Measured And Kept Indexes

These indexes were already measured in `docs/performance/db-indexing-effect-20260706.md` and are
present in `docs/sql/20260706_add_performance_indexes.sql`.

| Table | Index | Columns | Coverage |
|---|---|---|---|
| `schedules` | `idx_schedules_user_deleted_start_end` | `user_id, is_deleted, start_time, end_time` | User schedule range lookup |
| `todos` | `idx_todos_user_deleted_due_at` | `user_id, is_deleted, due_at` | User todo date/range lookup |
| `todos` | `idx_todos_alarm_lookup` | `is_alarm_sent, is_deleted, is_completed, is_all_day, due_at` | Todo alarm batch lookup |
| `user_medications` | `idx_user_medications_user_active_created_id` | `user_id, is_active, created_at, id` | User medication list and ordering |
| `user_medications` | `idx_user_medications_user_started_end` | `user_id, started_at, end_at` | Medication period overlap lookup |
| `journal_tag_logs` | `idx_journal_tag_logs_user_date_checked` | `user_id, journal_date, checked_at` | Journal tag period lookup |
| `community_boards` | `idx_community_boards_deleted_created` | `created_at, is_deleted` | Latest board page lookup |
| `community_boards` | `idx_community_boards_deleted_category_created` | `is_deleted, post_category, created_at` | Category board page lookup |
| `comments` | `idx_comments_post_deleted_created` | `post_id, is_deleted, created_at` | Post comment list lookup |

### Code-Declared Unique Constraints That Also Act As Indexes

| Table | Constraint columns | Coverage |
|---|---|---|
| `notification_subscriptions` | `user_id, endpoint` | Web push subscription dedupe |
| `notification_subscriptions` | `user_id, token` | Mobile push subscription dedupe |
| `notification_history` | `user_id, alarm_type, reference_id, alarm_scheduled_at` | Duplicate notification prevention |
| `external_calendar_events` | Provider event uniqueness | External event dedupe |
| `calendar_connections` | `user_id, provider` | Provider connection lookup |
| `medications` | `name` | Medication name uniqueness |
| `medication_dosages` | `medication_id, amount` | Medication dosage dedupe |
| `user_medication_schedules` | `user_medication_id, dose_time` | Schedule dedupe and schedule-owned lookup |
| `user_medication_logs` | `user_medication_schedule_id, taken_at` | Schedule-owned medication log lookup |
| `daily_status_logs` | `userId, date` | User daily status exact/range lookup |
| `daily_goals` | `userId, dailyGoal` | User goal dedupe |
| `daily_goal_logs` | `dailyGoalId, date` | Goal log exact/range lookup after goal lookup |
| `journal_tags` | `owner_key, category, name, tag_type` | Tag catalog dedupe |
| `memos` | `userId, journalDate` | User memo exact/range lookup |

### Tables With Query Patterns But No Explicit Performance Index Yet

These are the main 2nd-pass candidates. They should be measured before changes are kept.

| Table | Query pattern | Current coverage assessment | Candidate to measure |
|---|---|---|---|
| `medication_analysis_reports` | `user_id + generated_at desc`, `user_id + status`, `user_id + period_start + period_end` | No entity-declared performance index; FK index may cover only `user_id` if DB created one | `(user_id, generated_at)`, `(user_id, status)`, `(user_id, period_start, period_end)` |
| `admin_audit_logs` | latest audit list ordered by `created_at desc, id desc` | Primary key on UUID does not support latest-order query | `(created_at, id)` |
| `consultations` | user-scoped non-deleted date-range list | No entity-declared performance index | `(user_id, is_deleted, consultation_date)` |
| `onboarding_symptoms` | latest symptom before a timestamp per user | No entity-declared performance index | `(user_id, saved_at)` |
| `asrs_assessments` | latest/all assessments by user ordered by `completed_at` | No entity-declared performance index | `(user_id, completed_at)` |
| `users` | status pagination and withdrawal cleanup | No explicit status/withdrawal index in entity | `(user_status)`, `(user_status, withdrawal_at)` only if measured hot |
| `user_settings` | report/marketing notification batch scans | No explicit notification-flag indexes | Defer unless batch scans show pain; boolean-only indexes are likely weak |

### Step 1 Findings

- A first indexing pass has already covered the highest-volume schedule, todo, medication, journal
  tag, board, and comment queries.
- The remaining strong candidates are narrower admin/report/onboarding/consultation paths, not the
  original high-volume journal/schedule/todo paths.
- `user_medication_logs` should remain verify-only for now. The existing
  `UNIQUE(user_medication_schedule_id, taken_at)` was already considered covered in the 2026-07-06
  result, and user-scoped log queries depend on join order through medication schedules.
- Keyword search on board/notice/medication names is not a B-tree index problem when the query uses
  contains search or `LOWER(...) LIKE '%keyword%'`.
- Actual production/dev DB index state still needs `SHOW INDEX` verification because production uses
  manual SQL and local/dev may use Hibernate `ddl-auto: update`.

## Step 2. Query Pattern Mapping

Status: completed from code on 2026-07-10. Actual SQL capture is deferred to Step 3 because it
requires running the application or focused repository tests with SQL logging enabled.

### 2nd-Pass Candidate Map

| Candidate | Repository method | Call path | Predicate/order shape | Measurement priority |
|---|---|---|---|---|
| `medication_analysis_reports(user_id, generated_at)` | `MedicationAnalysisReportRepository.findByUser_IdOrderByGeneratedAtDesc` | `MedicationAnalysisService.listReports` | `WHERE user_id = ? ORDER BY generated_at DESC` | High if reports can accumulate per user |
| `medication_analysis_reports(user_id, period_start, period_end)` | `MedicationAnalysisReportRepository.findByUser_IdAndPeriodStartAndPeriodEnd` | `MedicationAnalysisService.createReport` | `WHERE user_id = ? AND period_start = ? AND period_end = ?` | High; also a data integrity candidate if duplicate reports per period are invalid |
| `medication_analysis_reports(user_id, status)` | `MedicationAnalysisReportRepository.findByUser_IdAndStatus` | Repository currently has method; no direct call found in service code reviewed | `WHERE user_id = ? AND status = ?` | Low until a caller is confirmed |
| `medication_analysis_reports(id, user_id)` | `MedicationAnalysisReportRepository.findByIdAndUser_Id` | `MedicationAnalysisService.getReport` | `WHERE id = ? AND user_id = ?` | Low; primary key on `id` should drive this query |
| `admin_audit_logs(created_at, id)` | `AdminAuditLogRepository.findAllByOrderByCreatedAtDescIdDesc` | `AdminAuditLogService.getLatest` | `ORDER BY created_at DESC, id DESC LIMIT ?` | Medium/high as audit logs grow |
| `consultations(user_id, is_deleted, consultation_date)` | `ConsultationRepository.findAllByUser_IdAndIsDeletedFalseAndConsultationDateBetweenOrderByConsultationDateAsc` | `ConsultationService.getConsultations` | `WHERE user_id = ? AND is_deleted = false AND consultation_date BETWEEN ? AND ? ORDER BY consultation_date ASC` | Medium |
| `onboarding_symptoms(user_id, saved_at)` | `OnboardingSymptomRepository.findTopByUserOrderBySavedAtDesc`; `findTopByUserIdAndSavedAtLessThanEqualOrderBySavedAtDesc` | `OnboardingService.getAiRecommendations`, `completeOnboarding`, `getHistoryDetail` | latest symptom per user; latest before assessment timestamp | Medium |
| `asrs_assessments(user_id, completed_at)` | `AsrsAssessmentRepository.findTopByUserOrderByCompletedAtDesc`; `findAllByUserWithAnswers`; `findFirstByUser_IdAndCompletedAtAfterOrderByCompletedAtAsc` | `OnboardingService.getAiRecommendations`, `getHistory`, `getHistoryDetail` | latest/all/next ASRS assessment per user ordered by completion time | Medium |
| `users(user_status, withdrawal_at)` | `UserRepository.findAllByUserStatus`; `findExpiredWithdrawnUsers` | Mail batch sender and permanent deletion flow | status paging; expired withdrawal cleanup | Medium, but measure only with realistic user counts |
| `user_settings(report_notification, id)` and `user_settings(marketing_notification, id)` | `UserSettingRepository.findAllByReportNotificationTrue`; `findAllByMarketingNotificationTrue`; `countMarketingTargets` | `WeeklyReportAlarmSender`, admin marketing notification sender | notification flag plus active user join, pageable by `id` | Low/medium; boolean flag selectivity may be poor |

### Step 2 Decisions

- Prioritize `medication_analysis_reports`, `admin_audit_logs`, and `consultations` for Step 3
  measurement. These have clear query shapes and no current explicit performance indexes.
- Measure `onboarding_symptoms` and `asrs_assessments` after the first three if data volume is
  expected to grow beyond a few rows per user.
- Defer `medication_analysis_reports(user_id, status)` until a production call path exists or a
  near-term feature needs it.
- Treat `findByIdAndUser_Id` as covered by the primary key unless `EXPLAIN` shows otherwise.
- Defer `user_settings` notification-flag indexes unless batch scans are observed as slow; standalone
  boolean-leading indexes often do not pay for their write/storage cost.

## Step 3. Baseline Measurement

Status: completed on 2026-07-10.

Completed:

1. Used dedicated local MySQL 8.4.10 container `attune-perf-mysql`.
2. Added 2nd-pass seed data in `docs/performance/db-indexing-2nd-pass-seed.sql`.
3. Ran `SHOW INDEX`, tabular `EXPLAIN`, and `EXPLAIN ANALYZE` for prioritized candidate queries.
4. Recorded baseline and after-measurement results in `docs/performance/db-indexing-effect-20260710.md`.

## Step 4. Candidate Index SQL

Status: completed on 2026-07-10.

Completed:

1. Created `docs/sql/20260710_add_performance_indexes.sql`.
2. Applied candidate indexes to the measurement DB only.
3. Kept production apply order in one SQL file.

## Step 5. After Measurement And Decision

Status: completed on 2026-07-10.

Completed:

1. Re-ran the same `EXPLAIN` and `EXPLAIN ANALYZE`.
2. Kept indexes with filesort removal, range/index scan improvements, or meaningful time reduction.
3. Deferred `users(user_status, withdrawal_at)` and notification-flag indexes.

## Step 6. Code And Docs Sync

Status: completed on 2026-07-10.

Completed:

1. Added kept indexes to entity `@Table(indexes = ...)`.
2. Added production DDL in `docs/sql/20260710_add_performance_indexes.sql`.
3. Updated `docs/db_schema.md`.
4. Created `docs/performance/db-indexing-effect-20260710.md`.

## Execution Results

### What Was Executed

1. Reviewed existing indexing work from 2026-07-06.
   - Existing high-volume indexes for schedules, todos, user medications, journal tag logs,
     community boards, and comments were already measured and kept.
   - This pass intentionally avoided duplicating those indexes.
2. Inventoried the remaining candidate tables from code and the measurement DB.
   - `medication_analysis_reports`
   - `admin_audit_logs`
   - `consultations`
   - `onboarding_symptoms`
   - `asrs_assessments`
   - `users`
   - `user_settings`
3. Seeded the empty candidate tables in the dedicated measurement DB.
   - Seed file: `docs/performance/db-indexing-2nd-pass-seed.sql`
   - Measurement DB: local Docker MySQL container `attune-perf-mysql`
   - MySQL version: 8.4.10
4. Ran baseline `SHOW INDEX`, `EXPLAIN`, and `EXPLAIN ANALYZE`.
5. Applied candidate indexes only to the measurement DB.
   - Candidate SQL: `docs/sql/20260710_add_performance_indexes.sql`
6. Re-ran the same `EXPLAIN` and `EXPLAIN ANALYZE` queries after index creation.
7. Kept only indexes with measured evidence.
8. Synchronized kept indexes into:
   - Entity `@Table(indexes = ...)`
   - `docs/sql/20260710_add_performance_indexes.sql`
   - `docs/db_schema.md`
   - `docs/performance/db-indexing-effect-20260710.md`

### Seed Row Counts

| Table | Rows |
|---|---:|
| `users` | 1,000 |
| `medication_analysis_reports` | 24,000 |
| `admin_audit_logs` | 100,000 |
| `consultations` | 100,000 |
| `onboarding_symptoms` | 10,000 |
| `asrs_assessments` | 10,000 |
| `user_settings` | 1,000 |

### Measured Index Decisions

| Area | Index | Before | After | Decision |
|---|---|---|---|---|
| Medication analysis report list | `idx_medication_analysis_reports_user_generated(user_id, generated_at)` | FK `user_id` lookup plus filesort, 2.7 ms | Reverse index scan, 0.901 ms | keep |
| Medication analysis report period lookup | `idx_medication_analysis_reports_user_period(user_id, period_start, period_end)` | 24 rows scanned then filtered, 1.57 ms | Exact composite index lookup, 0.026 ms | keep |
| Admin audit latest feed | `idx_admin_audit_logs_created_id(created_at, id)` | 100,000-row table scan plus filesort, 108 ms | Reverse index scan limited to 50 rows, 1.3 ms | keep |
| Consultation range list | `idx_consultations_user_deleted_date(user_id, is_deleted, consultation_date)` | FK `user_id` lookup plus filter/filesort, 1.82 ms | Composite range scan, 0.404 ms | keep |
| Latest onboarding symptom | `idx_onboarding_symptoms_user_saved(user_id, saved_at)` | FK `user_id` lookup plus filesort, 0.182 ms | Reverse index scan, 0.109 ms | keep |
| Onboarding symptom before timestamp | `idx_onboarding_symptoms_user_saved(user_id, saved_at)` | FK `user_id` lookup plus filter/filesort, 0.098 ms | Composite range scan, 0.029 ms | keep |
| ASRS history/latest lookup | `idx_asrs_assessments_user_completed(user_id, completed_at)` | FK `user_id` lookup plus filesort, 0.128 ms | Reverse index scan, 0.083 ms | keep |
| Next ASRS lookup | `idx_asrs_assessments_user_completed(user_id, completed_at)` | FK `user_id` lookup plus filter/filesort, 0.701 ms | Composite range scan, 0.057 ms | keep |

### Deferred Candidates

| Candidate | Reason |
|---|---|
| `users(user_status, withdrawal_at)` | The seed had only 1,000 users and no expired withdrawal rows. Baseline did not justify the index. |
| `user_settings` notification-flag indexes | Boolean-leading indexes are likely weak without evidence from slow batch scans. Deferred until batch metrics show pain. |
| `medication_analysis_reports(user_id, status)` | Repository method exists, but no active service call path was found in reviewed code. |

### Files Changed

Code:

- `src/main/java/attune/medicationAnalysis/domain/model/MedicationAnalysisReport.java`
- `src/main/java/attune/admin/audit/domain/AdminAuditLog.java`
- `src/main/java/attune/consultation/domain/model/Consultation.java`
- `src/main/java/attune/onboarding/domain/model/OnboardingSymptom.java`
- `src/main/java/attune/onboarding/domain/model/AsrsAssessment.java`

Docs and SQL:

- `docs/exec-plans/active/2026-07-10-db-index-optimization.md`
- `docs/performance/db-indexing-2nd-pass-seed.sql`
- `docs/performance/db-indexing-effect-20260710.md`
- `docs/sql/20260710_add_performance_indexes.sql`
- `docs/db_schema.md`

### Verification Result

- `.\gradlew.bat --no-daemon compileJava`: passed.
- `.\gradlew.bat --no-daemon test`: not completed in this session.
  - First run timed out after about 2 minutes.
  - A later run failed while Gradle tried to delete `build/test-results/test/binary/output.bin`,
    likely because a Java/Gradle process still held a file lock.
  - Gradle daemons were stopped with `.\gradlew.bat --stop`.
  - A final full test run timed out after about 5 minutes.
  - No test assertion failure was observed, but the full test suite remains unverified.

## Verification

Minimum verification:

```powershell
.\gradlew.bat --no-daemon test
```

Preferred if entity mappings or schema declarations change:

```powershell
.\gradlew.bat --no-daemon build
```

## Completion Criteria

- [x] Step 1 inventory is documented.
- [x] Step 2 query pattern mapping is complete.
- [x] Step 3 baseline measurements are recorded or explicitly blocked by missing DB access.
- [x] Step 4 candidate SQL exists only for measured candidates.
- [x] Step 5 after-measurement decisions are recorded.
- [x] Step 6 code/docs are synchronized for kept indexes.
