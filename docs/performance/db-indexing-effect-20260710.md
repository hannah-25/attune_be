# DB Indexing Effect Measurement - 2026-07-10

## Environment

- Measurement DB: local Docker container `attune-perf-mysql`
- MySQL: 8.4.10
- Database: `attune_perf`
- Base seed: `docs/performance/db-indexing-effect-seed.sql`
- 2nd-pass seed: `docs/performance/db-indexing-2nd-pass-seed.sql`

## Seed Row Counts

| Table | Rows |
|---|---:|
| users | 1,000 |
| medication_analysis_reports | 24,000 |
| admin_audit_logs | 100,000 |
| consultations | 100,000 |
| onboarding_symptoms | 10,000 |
| asrs_assessments | 10,000 |
| user_settings | 1,000 |

Measured user: `UNHEX(MD5('attune-user-500'))`.

## Baseline Index Inventory

| Table | Existing relevant indexes before this pass |
|---|---|
| `medication_analysis_reports` | `PRIMARY(id)`, FK-backed `user_id` |
| `admin_audit_logs` | `PRIMARY(id)` |
| `consultations` | `PRIMARY(id)`, FK-backed `user_id` |
| `onboarding_symptoms` | `PRIMARY(id)`, FK-backed `user_id` |
| `asrs_assessments` | `PRIMARY(id)`, FK-backed `user_id` |
| `users` | `PRIMARY(id)` |
| `user_settings` | `PRIMARY(user_id)` |

## Candidate Indexes

The following SQL was applied only to the measurement DB:

- `docs/sql/20260710_add_performance_indexes.sql`

## Summary

| Query | Before key | After key | Before time | After time | Before rows | After rows | Decision |
|---|---|---|---:|---:|---:|---:|---|
| Report list by generated date | FK `user_id` + filesort | `idx_medication_analysis_reports_user_generated` | 2.7 ms | 0.901 ms | 24 returned | 24 returned | keep |
| Report lookup by period | FK `user_id` + filter | `idx_medication_analysis_reports_user_period` | 1.57 ms | 0.026 ms | 24 scanned / 1 returned | 1 returned | keep |
| Admin audit latest | table scan + filesort | `idx_admin_audit_logs_created_id` | 108 ms | 1.3 ms | 100,000 scanned | 50 scanned/returned | keep |
| Consultations date range | FK `user_id` + filter + filesort | `idx_consultations_user_deleted_date` | 1.82 ms | 0.404 ms | 100 scanned / 66 returned | 66 returned | keep |
| Latest onboarding symptom | FK `user_id` + filesort | `idx_onboarding_symptoms_user_saved` | 0.182 ms | 0.109 ms | 10 scanned / 1 returned | 1 returned | keep, limited but removes filesort |
| Onboarding symptom before time | FK `user_id` + filter + filesort | `idx_onboarding_symptoms_user_saved` | 0.098 ms | 0.029 ms | 10 scanned / 5 filtered / 1 returned | range scan / 1 returned | keep |
| ASRS assessments by completed date | FK `user_id` + filesort | `idx_asrs_assessments_user_completed` | 0.128 ms | 0.083 ms | 10 returned | 10 returned | keep, limited but removes filesort |
| Next ASRS assessment | FK `user_id` + filter + filesort | `idx_asrs_assessments_user_completed` | 0.701 ms | 0.057 ms | 10 scanned / 5 filtered / 1 returned | range scan / 1 returned | keep |
| Expired withdrawn users | table scan | unchanged | 1.03 ms | not changed | 1,000 scanned | unchanged | defer |

## Decisions

- Keep `idx_medication_analysis_reports_user_generated`.
  - Removes filesort for report list queries and supports newest-first access.
- Keep `idx_medication_analysis_reports_user_period`.
  - Converts report period lookup from user-prefix scan plus filter to exact composite index lookup.
- Keep `idx_admin_audit_logs_created_id`.
  - Converts latest audit feed from 100,000-row table scan and filesort to reverse index scan limited
    to the requested page size.
- Keep `idx_consultations_user_deleted_date`.
  - Converts user consultation range lookup to composite range scan and removes filesort.
- Keep `idx_onboarding_symptoms_user_saved`.
  - Benefit is small at 10 rows per user, but it directly supports latest and latest-before-time
    access without sorting.
- Keep `idx_asrs_assessments_user_completed`.
  - Benefit is small at 10 rows per user, but it supports both newest-first history and next
    assessment lookup.
- Defer `users(user_status, withdrawal_at)`.
  - Current seed has only 1,000 users and no expired withdrawal rows, so the measurement does not
    justify adding the index yet.
- Defer `user_settings` notification-flag indexes.
  - Boolean-leading indexes are unlikely to pay for themselves without evidence from slower batch
    scans.

## Follow-Up

- Keep the SQL file, entity `@Table(indexes = ...)` declarations, and `docs/db_schema.md` in sync.
- Remeasure `users` and `user_settings` only if production-like row counts or batch timings show
  those scans becoming hot.

## Implementation Status

Implemented in code and docs:

- Entity indexes:
  - `MedicationAnalysisReport`: `idx_medication_analysis_reports_user_generated`,
    `idx_medication_analysis_reports_user_period`
  - `AdminAuditLog`: `idx_admin_audit_logs_created_id`
  - `Consultation`: `idx_consultations_user_deleted_date`
  - `OnboardingSymptom`: `idx_onboarding_symptoms_user_saved`
  - `AsrsAssessment`: `idx_asrs_assessments_user_completed`
- Production SQL:
  - `docs/sql/20260710_add_performance_indexes.sql`
- Schema documentation:
  - `docs/db_schema.md`
- Execution plan:
  - `docs/exec-plans/active/2026-07-10-db-index-optimization.md`

Verification:

- `.\gradlew.bat --no-daemon compileJava`: passed.
- `.\gradlew.bat --no-daemon test`: not completed during this session.
  - One run timed out.
  - One run hit a Gradle test result file-lock cleanup error on
    `build/test-results/test/binary/output.bin`.
  - After stopping Gradle daemons, a later full test run still timed out after about 5 minutes.
  - No assertion failure was observed, but the full test suite remains unverified.
