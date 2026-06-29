# Sentry Baseline

초기 Sentry 통합은 의도적으로 보수적이고 비용을 제한한 형태로 구성한다.

## 런타임 정책

- 의존성: `io.sentry:sentry-spring-boot-4-starter`.
- 기본값: `sentry.enabled=${SENTRY_ENABLED:false}`.
- 운영 활성화에는 `SENTRY_ENABLED=true`, `SENTRY_DSN`, `APP_RELEASE`가 필요하다.
- secret은 GitHub Secrets, `application-secret.yml`, 환경 변수를 통해 주입한다. DSN은 커밋하지 않는다.
- 1차 범위는 오류 이벤트로 한정한다. tracing, profiling은 기본 비활성이다.
- `@RestControllerAdvice`(`GlobalExceptionHandler`)가 모든 예외를 resolve 하므로 `SentryExceptionResolver` 경로로는 오류가 잡히지 않는다. 따라서 Sentry log appender를 ERROR 레벨로 켜서 `log.error`(500대)만 이벤트로 캡처한다. 4xx(`log.info`/`warn`)는 이벤트로 가지 않는다.

## PII 통제

기본 옵션:

- `send-default-pii=false`
- `max-request-body-size=none`
- `traces-sample-rate=0.0`
- `profiles-sample-rate=0.0`
- `sentry.logging.enabled=true`, `sentry.logging.minimum-event-level=error` (handled 예외 캡처용, ERROR 한정)

또한 애플리케이션은 오류 이벤트(`BeforeSendCallback`)와 트랜잭션 이벤트(`BeforeSendTransactionCallback`)
양쪽에서 다음 항목을 제거한다. 트랜잭션 콜백은 향후 `traces-sample-rate>0`으로 tracing을 켜더라도
PII가 새지 않도록 하는 defense-in-depth다. 두 콜백 빈은 `sentry.enabled=true`일 때만 등록된다(`@ConditionalOnProperty`).

- Sentry user
- request body
- query string
- cookies
- `X-Request-Id`를 제외한 request header

## 후속 작업

- 운영용 Sentry DSN을 발급한다.
- DSN을 `APPLICATION_SECRET_YML` 또는 런타임 환경에 추가한다.
- DSN 주입이 확인된 뒤에만 `SENTRY_ENABLED=true`로 설정한다.
- 알림을 신뢰하기 전에 Sentry 프로젝트에서 sanitize된 테스트 이벤트 1건을 검증한다.
