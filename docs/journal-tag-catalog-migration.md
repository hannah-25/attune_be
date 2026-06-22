# Journal Tag Catalog Migration

> **Superseded:** 실제 이용자가 없는 상태에서 단계적 dual-write 대신 직접 전환하기로 결정했다.
> 신규 구현과 배포는 `docs/journal-tag-direct-cutover-plan.md`를 기준으로 진행한다.
> 이 문서는 기존 설계의 배경과 이력 확인 용도로만 유지한다.

## Goal

Replace copied per-user default tags with a shared tag catalog and per-user preferences without changing current APIs during the migration.

## Current Migration State

- Phase 0 completed in application code:
  - duplicate activation event removed
  - default tag copy made idempotent by name
  - copy listener runs after transaction commit
  - startup migration disabled unless explicitly enabled
- Phase 1 audit SQL added:
  - `docs/sql/20260615_audit_legacy_journal_tags.sql`
- Phase 2 additive schema SQL added:
  - `docs/sql/20260615_create_journal_tag_catalog.sql`
- Phase 3 backfill SQL added:
  - `docs/sql/20260615_backfill_journal_tag_catalog.sql`
- Phase 4 dual-write application support added:
  - enable with `app.journal-tag-catalog.dual-write-enabled=true`
  - tag creation, copied defaults, visibility, deletion, and new log rows write to both structures
- Phase 5 compatible catalog-read support added:
  - enable with `app.journal-tag-catalog.read-enabled=true`
  - external API `tagId` remains the legacy per-user tag ID
  - tag lists, journal active tags, and onboarding recommendations read catalog preferences
- Catalog-native transition APIs added:
  - create, list, and preference operations use `catalogTagId`
  - check and uncheck operations use `catalogTagId`
  - first use of an unmapped system tag lazily creates one legacy compatibility tag instead of copying all defaults
  - catalog delete disables system tags per user and deactivates user-created tags
- Continuous dual-write verification SQL added:
  - `docs/sql/20260615_verify_journal_tag_catalog_dual_write.sql`

The Phase 2 migration adds tables and nullable log columns only. Existing application reads and writes remain unchanged.

The dual-write application maps the new tables even while the feature flag is disabled. Because production uses `ddl-auto=validate`, the Phase 2 schema SQL must be applied before deploying the dual-write build.

## Deployment Order

1. Run the audit SQL against a production snapshot.
2. Resolve duplicate default tags and orphan logs before backfill.
3. Save all audit result sets and legacy table row counts.
4. Run the catalog schema SQL once.
5. Verify the new tables, columns, indexes, and constraints.
6. Run the backfill SQL against a production snapshot and verify every unmapped count is zero.
7. Run the verified backfill SQL in production.
8. Deploy the dual-write application with the feature flag disabled.
9. Enable `app.journal-tag-catalog.dual-write-enabled=true` after schema and backfill verification.
10. Rerun the backfill verification query after enabling dual-write. Any non-zero unmapped count is a release blocker.
11. Enable `app.journal-tag-catalog.read-enabled=true` only after dual-write has remained healthy and verification remains zero.
12. Run the dual-write verification SQL regularly, setting the environment's actual activation timestamp.
13. Migrate frontend create, preference, check, uncheck, and delete flows to the catalog API.
14. Disable new-user default tag copying with `app.journal-tag-catalog.copy-defaults-enabled=false`.
15. Remove legacy tag APIs and columns only after catalog API adoption and a final zero-mismatch verification.

Catalog read is automatically treated as disabled unless dual-write is also enabled.

After the frontend migrates list, preference, check, uncheck, and delete flows to catalog APIs, set:

`APP_JOURNAL_TAG_CATALOG_COPY_DEFAULTS_ENABLED=false`

This stops copying all default tags for newly activated users. Legacy compatibility tags are then created only when required by the temporary legacy NOT NULL log columns.

The environment variable form of the feature flag is:

`APP_JOURNAL_TAG_CATALOG_DUAL_WRITE_ENABLED=true`

`APP_JOURNAL_TAG_CATALOG_READ_ENABLED=true`

When dual-write is enabled, a missing legacy-to-catalog mapping fails the write transaction instead of silently creating incomplete catalog data.

## Residual Risk

The first catalog-native check of a system tag without a user mapping creates one legacy compatibility tag because legacy log foreign keys are still required. Concurrent first checks for the same user and catalog tag can create duplicate compatibility tags until the legacy mapping schema can enforce one mapping per user/category/catalog tag.

## API Cutover Decision

Catalog-read cannot be enabled transparently because current API `tagId` values are legacy per-user tag IDs. Before removing copied default tags, choose and coordinate one of these contracts:

1. Keep legacy IDs as external IDs and maintain a compatibility mapping layer.
2. Change API `tagId` values to catalog IDs and update frontend tag check, toggle, delete, onboarding, and journal flows.

Do not remove legacy tag copies or make legacy log tag columns nullable until this contract is implemented and verified.

The compatible catalog-read implementation chooses option 1 for the transition period. It only exposes catalog tags that already have a legacy mapping for the user. A newly inserted system catalog tag cannot be exposed through the legacy-ID API until a compatibility mapping exists.

## Catalog Identity Rules

- System tag: `(SYSTEM, zero owner key, category, name, tag_type)`
- User tag: `(USER, owner_user_id, category, name, tag_type)`
- Side-effect tags use `tag_type = 'NONE'`.
- User preferences are unique by `(user_id, journal_tag_id)`.

MySQL permits multiple `NULL` values in a normal unique index, so `journal_tags.owner_key` is an explicit non-null identity column:

- system tags use a zero `BINARY(16)` owner key
- user tags use the same value for `owner_key` and `owner_user_id`

A CHECK constraint enforces this rule. This makes duplicate system tags impossible while retaining a nullable owner foreign key.

## Rollback

Before dual-write is enabled, rollback is limited to dropping the nullable log columns and the three new tables. Existing tag and log data is not modified by the Phase 2 migration.

The Phase 3 backfill only writes to the new schema and nullable additive log columns. It does not modify legacy tag IDs or legacy log tag IDs.

Do not drop the new schema after backfill or dual-write begins without first verifying that no production process depends on it.
