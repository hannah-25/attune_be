# Sentry Baseline

Initial Sentry integration is intentionally conservative and cost-limited.

## Runtime Policy

- Dependency: `io.sentry:sentry-spring-boot-4-starter`.
- Default: `sentry.enabled=${SENTRY_ENABLED:false}`.
- Production enablement requires `SENTRY_ENABLED=true`, `SENTRY_DSN`, and `APP_RELEASE`.
- Secrets must be injected through GitHub Secrets, `application-secret.yml`, or environment variables. Do not commit DSNs.
- Error events are the first scope. Sentry log appender, tracing, and profiling are disabled by default.

## PII Controls

Default options:

- `send-default-pii=false`
- `max-request-body-size=none`
- `traces-sample-rate=0.0`
- `profiles-sample-rate=0.0`
- `sentry.logging.enabled=false`

The application also registers a `BeforeSendCallback` that removes:

- Sentry user
- request body
- query string
- cookies
- request headers except `X-Request-Id`

## Follow-Up

- Issue the production Sentry DSN.
- Add DSN to `APPLICATION_SECRET_YML` or runtime environment.
- Set `SENTRY_ENABLED=true` only after DSN injection is confirmed.
- Verify one sanitized test event in the Sentry project before relying on alerts.
