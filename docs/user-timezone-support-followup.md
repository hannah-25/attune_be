# User timezone support follow-up

## Problem

The application currently operates with an Asia/Seoul JVM and database timezone. This keeps the
existing service behavior consistent, but it cannot correctly represent every user's intent when a
user lives in or travels to another timezone.

Hardcoding every reminder to Asia/Seoul is not a complete solution. A medication reminder configured
for 09:00 may mean either:

- 09:00 in the user's current local timezone; or
- the instant corresponding to 09:00 in the timezone where the reminder was created.

That policy must be explicit and user-controlled.

## Target model

- Store machine timestamps and integration boundaries as UTC `Instant`.
- Store a user's IANA timezone ID, such as `Asia/Seoul` or `America/New_York`.
- Keep wall-clock reminder values such as medication dose time separate from absolute timestamps.
- Preserve timezone or offset data received from external calendar providers.
- Convert to a local date and time only at user-facing or scheduling boundaries.

## Travel policy

Provide a reminder policy per user or schedule:

1. `FOLLOW_LOCAL_TIME`: a 09:00 reminder fires at 09:00 in the user's current timezone.
2. `KEEP_ORIGINAL_ZONE`: a 09:00 Asia/Seoul reminder keeps the same original-zone schedule abroad.

The product must define how the current timezone is updated. Options include an explicit user setting,
a client-reported timezone with confirmation, or a temporary travel override.

## Implementation outline

1. Add an IANA timezone field to user settings with validation through `ZoneId.of`.
2. Add reminder timezone policy and original timezone where required.
3. Refactor schedulers to calculate due reminders from `Instant.now()` and user-specific zones.
4. Change persisted absolute timestamps to UTC-compatible types through a controlled migration.
5. Preserve Google Calendar event timezone/offset instead of flattening everything to server local time.
6. Add DST transition, timezone-change, and international travel tests.

## Completion criteria

- Users can select or confirm their timezone.
- Reminder behavior during travel is documented and configurable.
- DST gaps and overlaps have deterministic behavior.
- Server or container timezone changes do not alter reminder delivery.
- Calendar synchronization preserves the source event's intended instant and timezone.
