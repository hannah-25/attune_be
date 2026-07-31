# Frontend Journal Tag Catalog Migration

## Current Status

The existing journal tag APIs continue to use legacy per-user `tagId` values. No immediate frontend change is required while the catalog feature flags remain disabled.

The backend now provides a parallel catalog API for gradual frontend migration.

## New Catalog API

### List Tags

`GET /v1/journals/catalog-tags?category=CONDITION`

Supported categories:

- `CONDITION`
- `SIDE_EFFECT`
- `TROUBLE`

Response:

```json
[
  {
    "catalogTagId": 10,
    "legacyTagId": 42,
    "category": "CONDITION",
    "name": "calm",
    "tagType": "CALM",
    "scope": "SYSTEM",
    "enabled": true,
    "visible": true
  }
]
```

`legacyTagId` is nullable. A newly added system catalog tag may not have a legacy ID.

### Update User Preference

`PATCH /v1/journals/catalog-tags/{catalogTagId}/preference`

```json
{
  "enabled": true,
  "visible": false
}
```

The list endpoint returns disabled tags too, allowing users to re-enable them.

### Create User Tag

`POST /v1/journals/catalog-tags`

```json
{
  "category": "CONDITION",
  "name": "focused",
  "tagType": "USER_INPUT",
  "visible": true
}
```

For `SIDE_EFFECT`, send `"tagType": "NONE"`.

Creating a tag with the same category, name, and type as an active system tag returns `409 Conflict`. Re-enable or update the existing system tag preference instead.

### Check Tag

`POST /v1/journals/catalog-tags/{catalogTagId}/checks`

No request body is required.

```json
{
  "catalogTagId": 10,
  "category": "CONDITION",
  "checkedAt": "2026-06-15T19:20:00"
}
```

### Uncheck Tag

`DELETE /v1/journals/catalog-tags/{catalogTagId}/checks?date=2026-06-15`

This removes checks for the catalog tag on the requested date.

### Delete or Disable Tag

`DELETE /v1/journals/catalog-tags/{catalogTagId}?journalDate=2026-06-15`

- System tag: disables the tag only for the current user.
- User tag: deactivates the user-created catalog tag.
- Both cases remove checks from `journalDate` onward.

## Required Frontend Model Changes

Introduce a catalog tag model containing:

```ts
type JournalTagCategory = "CONDITION" | "SIDE_EFFECT" | "TROUBLE";
type JournalTagScope = "SYSTEM" | "USER";

interface CatalogJournalTag {
  catalogTagId: number;
  legacyTagId: number | null;
  category: JournalTagCategory;
  name: string;
  tagType: string;
  scope: JournalTagScope;
  enabled: boolean;
  visible: boolean;
}
```

Use `catalogTagId` for preference editing and React/UI list keys.

Use `catalogTagId` for catalog-native preference, check, uncheck, and delete APIs. `legacyTagId` remains available only during the compatibility period.

## Recommended Migration Sequence

1. Add the catalog tag type and API client methods, including catalog-native tag creation.
2. Migrate tag-management screens to the catalog list and preference endpoint.
3. Migrate journal check/uncheck actions to the catalog check APIs using `catalogTagId`.
4. Migrate delete actions to the catalog delete API.
5. Stop depending on `legacyTagId` in UI behavior.
6. Remove remaining legacy tag API usage after backend confirms the compatibility period has ended.

## UI Behavior

- `scope = SYSTEM`: label delete actions as disable/remove-from-my-list because the shared tag is not globally deleted.
- `scope = USER`: delete actions deactivate the user-created catalog tag.
- `enabled = false`: keep visible in tag-management screens, hide from normal journal entry screens.
- `visible = false`: keep enabled but hide from the default journal quick-entry area.

## Feature Flag Dependency

The catalog API is available only when both backend flags are enabled:

- `APP_JOURNAL_TAG_CATALOG_DUAL_WRITE_ENABLED=true`
- `APP_JOURNAL_TAG_CATALOG_READ_ENABLED=true`

Catalog API error behavior:

- inaccessible or missing catalog tag: `404 Not Found`
- disabled tag check or invalid `tagType`: `400 Bad Request`
- duplicate catalog identity: `409 Conflict`

After the frontend has migrated all tag flows, backend can stop copying default tags for new users with:

- `APP_JOURNAL_TAG_CATALOG_COPY_DEFAULTS_ENABLED=false`
