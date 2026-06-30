# Execution Plan: Targeted Redis Cache

- Status: active
- Author / date: Codex / 2026-07-01
- Related issue/PR: TBD

## Goal

Add Redis only where backend-side caching has clear value over frontend caching for an expected user base of about 1,000 users.

## Background

The backend already uses Redis for refresh-token session state. Broad Redis caching would add invalidation and failure-mode complexity, so this plan intentionally limits Redis usage to data that is shared, stable, and cheap to invalidate.

## Current State

- Refresh-token session state is stored in Redis through `UserAuthCacheRepository`.
- OAuth JWKS uses Caffeine. This plan does not migrate JWKS to Redis.
- Medication reference data is read from MySQL on every medication list/search/detail request.
- System journal tags are read repeatedly when building journal active-tag responses.
- User-specific dynamic data such as journal details, calendars, settings, community posts, and alarm candidates remains uncached at the backend.

## Scope

1. Keep the existing Redis-backed refresh-token session flow.
2. Add a small Redis JSON cache helper with safe DB fallback.
3. Cache medication reference responses:
   - all medication search/list results
   - medication search results by normalized keyword
   - medication detail by medication ID
4. Cache system journal tag source data by category.
5. Add focused tests for cache hit/miss/fallback behavior.
6. Update docs if behavior or operational assumptions change.

## Out Of Scope

- OAuth JWKS Redis migration.
- Calendar event Redis cache.
- User-specific journal detail/bulk Redis cache.
- User profile/settings Redis cache.
- Community board/comment Redis cache.
- Alarm scheduler candidate Redis cache.
- Payment-specific token blacklist.
- Stateless JWT refresh-token migration.

## Related Docs

- `AGENTS.md`
- `docs/agent/agent-workflow.md`
- `docs/architecture/system-overview.md`
- `docs/token-refresh-api-spec.md`

## Related Code

- `src/main/java/attune/auth/domain/repository/UserAuthCacheRepository.java`
- `src/main/java/attune/medication/application/MedicationService.java`
- `src/main/java/attune/journal/application/JournalTagService.java`
- `src/main/java/attune/common/config/RedisConfig.java`
- `src/main/java/attune/common/config/CacheConfig.java`

## Implementation Steps

1. [x] Add a Redis JSON cache helper under `attune.common.cache`.
2. [x] Apply the helper to medication reference read paths.
3. [x] Apply the helper to system journal tag reads only.
4. [x] Add or update unit tests for medication and journal cache behavior.
5. [x] Run targeted tests.
6. [x] Document cache keys, TTLs, and fallback assumptions.
7. [x] Run the full test suite before PR if runtime permits.

## Verification

- `./gradlew test --tests "attune.medication.application.MedicationServiceTest"`
- `./gradlew test --tests "attune.journal.application.JournalTagServiceTest"` if present or added.
- `./gradlew test` before PR if runtime permits.

## Risks

- Redis deserialization errors can break read APIs if not handled as cache misses.
- Caching user-specific tag responses would require broad invalidation; this plan avoids that.
- Long TTLs can show stale medication/tag reference data if future admin edit APIs are added without eviction.

## Rollback

- Remove cache helper usage from medication and journal services.
- Keep Redis refresh-token session code unchanged.
- Delete new tests and docs if the feature is reverted.

## Decisions

- 2026-07-01: Do not migrate OAuth JWKS from Caffeine to Redis in this plan.
- 2026-07-01: Do not cache user-specific dynamic data in Redis for the initial implementation.
- 2026-07-01: Use Redis only for backend-shared stable data, plus the existing auth session use case.

## Completion Criteria

- [x] Medication reference reads use Redis with DB fallback.
- [x] System journal tag source reads use Redis with DB fallback.
- [x] Redis failures and malformed cached values do not fail the read API.
- [x] Tests cover cache miss, cache hit, and fallback behavior.
- [x] Relevant docs mention the targeted cache policy.
- [x] Full test suite passes before PR.

## Post-Work Documentation Updates

- [x] `docs/architecture/system-overview.md`
- [ ] `docs/engineering/observability.md` if operational notes are added.
