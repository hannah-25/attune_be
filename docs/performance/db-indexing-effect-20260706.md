# DB Indexing Effect Measurement - 2026-07-06

## Environment

- Measurement DB: local Docker container `attune-perf-mysql`
- MySQL: 8.4.10
- Database: `attune_perf`
- Seed: `docs/performance/db-indexing-effect-seed.sql`

## Seed Row Counts

| Table | Rows |
|---|---:|
| users | 1,000 |
| schedules | 100,000 |
| todos | 100,000 |
| user_medications | 2,000 |
| user_medication_schedules | 4,000 |
| user_medication_logs | 120,000 |
| daily_status_logs | 120,000 |
| memos | 120,000 |
| journal_tag_logs | 300,000 |
| community_boards | 50,000 |
| comments | 202,000 |

Measured user: `UNHEX(MD5('attune-user-500'))`. Hot post: `community_boards.id = 123`.

## Summary

| Query | Before key | After key | Before time | After time | Before rows | After rows | Decision |
|---|---|---|---:|---:|---:|---:|---|
| Schedule range, manualOnly null | `NULL` | `idx_schedules_user_deleted_start_end` | 142 ms | 0.167 ms | 100,000 scanned | 96 scanned / 4 returned | keep |
| Schedule range, manualOnly true | `NULL` | `idx_schedules_user_deleted_start_end` | 98.4 ms | 0.112 ms | 100,000 scanned | 96 scanned / 4 returned | keep |
| Schedule range, manualOnly false | `NULL` | `idx_schedules_user_deleted_start_end` | 106 ms | 0.104 ms | 100,000 scanned | 96 scanned / 0 returned | keep |
| Todo date range | `NULL` | `idx_todos_user_deleted_due_at` | 64.7 ms | 0.173 ms | 100,000 scanned | 8 scanned / 8 returned | keep |
| Todo alarm candidates | `idx_todos_alarm_lookup` | unchanged | 0.394 ms | not changed | 0 returned | unchanged | already covered |
| User medications list | FK `user_id` | `idx_user_medications_user_active_created_id` | 4.78 ms | 5.68 ms | 2 scanned | 2 scanned | keep, removes filesort but benefit is limited by small per-user data |
| User medications overlap | FK `user_id` | `idx_user_medications_user_started_end` | 1.68 ms | 0.107 ms | 2 scanned | 2 scanned / 1 returned | keep |
| Medication logs list | existing unique schedule/taken index | unchanged | 27.7 ms | not changed | 120 returned | unchanged | already covered |
| Medication logs count | existing unique schedule/taken index | unchanged | 0.282 ms | not changed | 120 counted | unchanged | already covered |
| Daily status range | existing `UNIQUE(user_id, date)` | unchanged | 23.1 ms | not changed | 30 returned | unchanged | already covered |
| Memo range | existing `UNIQUE(user_id, journal_date)` | unchanged | 16.3 ms | not changed | 30 returned | unchanged | already covered |
| Journal tag range | `NULL` | `idx_journal_tag_logs_user_date_checked` | 353 ms | 1.5 ms | 300,000 scanned | 90 scanned / 90 returned | keep |
| Board page | `NULL` | `idx_community_boards_deleted_created` | 205 ms | 0.706 ms | 50,000 scanned | 20 scanned | keep |
| Board count | `NULL` | `idx_community_boards_deleted_created` | 22.4 ms | 31.6 ms | 50,000 scanned | 47,480 index rows | keep for page query; count-only improvement not shown |
| Board category page | `NULL` | `idx_community_boards_deleted_category_created` | 58.3 ms | 0.468 ms | 50,000 scanned | 20 scanned | keep |
| Board category count | `NULL` | `idx_community_boards_deleted_category_created` | 21.8 ms | 7.38 ms | 50,000 scanned | 11,910 index rows | keep |
| Board keyword page | `NULL` | `idx_community_boards_deleted_created` | 61.9 ms | 189 ms | 50,000 scanned | 47,480 index rows | defer; B-tree does not solve `LOWER(...) LIKE '%keyword%'` |
| Comments by post | FK `post_id` | `idx_comments_post_deleted_created` | 12.4 ms | 6.07 ms | 2,001 scanned / 1,901 returned | 1,901 returned | keep |

## Notes

- `schedules.start_time < :endDate AND end_time >= :startDate` remains a double-range predicate. MySQL uses the index through `start_time` and applies `end_time` as an index condition.
- `CommunityBoardRepository.searchPosts` with keyword still needs a different search strategy. The measured B-tree indexes are kept for non-keyword listing and category filtering only.
- `comments.post_id` was previously backed by a single-column FK index. The composite `(post_id, is_deleted, created_at)` covers the FK leftmost prefix and the measured read query.

## Community Board Index Order Comparison - 2026-07-07

Additional measurement compared the two possible column orders for
`idx_community_boards_deleted_created` against the same seed data.

- Dataset: `community_boards` 50,000 rows, 2,520 deleted rows.
- Deleted ratio: 5.04%.
- Query: latest board page, equivalent to
  `WHERE b.is_deleted = 0 ORDER BY b.created_at DESC LIMIT 20`, with the `users` join.
- Count query: `SELECT COUNT(b.id) FROM community_boards b WHERE b.is_deleted = 0`.
- Each variant was created with `ALTER TABLE`, followed by `ANALYZE TABLE community_boards`.
- Warm runs are recorded below; first cold-cache outliers were ignored.

| Variant | Page access path | Page time | Page actual rows | Count access path | Count time | Count rows | Notes |
|---|---|---:|---:|---|---:|---:|---|
| `(is_deleted, created_at)` | index range scan on `is_deleted = 0`, reverse | 0.402-0.520 ms | 20 scanned / 20 returned | covering lookup on same index | 22.5-43.1 ms | 47,480 | Equality-first order. Best when deleted rows become a large share of the table. |
| `(created_at, is_deleted)` | reverse index scan on `created_at`, filter `is_deleted = 0` | 0.339-0.348 ms | 21 scanned / 20 returned | optimizer chose `idx_community_boards_deleted_category_created` | 26.2-36.8 ms | 47,480 | With 5.04% deleted rows, only one extra row was scanned for the latest page and the index remains more reusable for latest-first board queries. |

Decision: keep `idx_community_boards_deleted_created` as `(created_at, is_deleted)`.

Reason: the target hot path is latest-first pagination, and the measured 5.04% deleted ratio means
the `created_at`-first index scans almost the same number of rows as the equality-first index for
the first page. The page-query timings are effectively equivalent at this dataset size, while
`created_at` first is more reusable for latest-first access patterns. If the production deleted
ratio grows substantially, especially beyond roughly 20-30%, remeasure and consider changing the
index back to `(is_deleted, created_at)`.

### Larger Dataset Check

The same comparison was repeated after expanding only `community_boards` from 50,000 to 500,000
rows. The extra rows used the same deterministic distribution as the original seed; comments were
not expanded because the board page query does not read them.

- Dataset: `community_boards` 500,000 rows, 25,184 deleted rows.
- Deleted ratio: 5.04%.
- Query: same latest board page query, `LIMIT 20`.
- Warm runs are recorded below; first cold-cache outliers were ignored.

| Variant | Page access path | Page time | Page actual rows | Count access path | Count time | Count rows | Notes |
|---|---|---:|---:|---|---:|---:|---|
| `(created_at, is_deleted)` | reverse index scan on `created_at`, filter `is_deleted = 0` | 0.132-0.172 ms | 23 scanned / 20 returned | optimizer chose `idx_community_boards_deleted_category_created` | 120-130 ms | 474,816 | Still scans only a few extra rows at 5.04% deleted. |
| `(is_deleted, created_at)` | index range scan on `is_deleted = 0`, reverse | 0.291-0.307 ms | 20 scanned / 20 returned | optimizer chose `idx_community_boards_deleted_category_created` | 116-136 ms | 474,816 | Reads exactly 20 board rows for the page, but was not faster in warm runs. |

Larger-dataset decision: keep `(created_at, is_deleted)`.

Reason: increasing the table from 50,000 to 500,000 rows did not make the `created_at`-first index
scan materially more rows for the latest page. With a stable 5.04% deleted ratio, the limiting
factor remains the number of newest rows needed to satisfy `LIMIT 20`, not the total table size.
