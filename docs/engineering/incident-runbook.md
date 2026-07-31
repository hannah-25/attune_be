# Incident Runbook

운영 장애가 발생했을 때 Sentry, CloudWatch Logs, Actuator, Micrometer 지표를 어떤 순서로 확인할지 정리한다.

이 문서는 장애 원인 조사를 빠르게 시작하기 위한 1차 절차다. 사용자 콘텐츠, 토큰, 이메일, 외부 API 키, 클라이언트 시크릿, AI 프롬프트/응답 본문은 로그나 이슈에 옮기지 않는다. 식별이 필요하면 `requestId`, 내부 `userId`, `connectionId`, report id처럼 민감하지 않은 식별자만 사용한다.

## 공통 원칙

1. Sentry에서 500 계열 이벤트와 stack trace를 확인한다.
2. 이벤트나 응답 헤더에서 `requestId`를 확보한다.
3. CloudWatch Logs에서 같은 `requestId`로 요청 흐름을 검색한다.
4. 외부 의존성 문제인지, DB/JPA 문제인지, 애플리케이션 검증 실패인지 분리한다.
5. Actuator readiness와 관련 custom metric을 함께 확인한다.
6. 사용자 응답 메시지와 내부 장애 원인을 분리해서 기록한다.

## 공통 진입점

Actuator 엔드포인트는 dev/prod에서 public 앱 포트와 분리된 `127.0.0.1:8081` management server에 바인딩된다. 운영 환경에서는 EC2 내부에서 `curl`로 확인하거나 SSH 터널링을 통해 접근한다.

| 대상 | 확인 경로 |
|------|-----------|
| Readiness | `GET http://127.0.0.1:8081/actuator/health/readiness` |
| Liveness | `GET http://127.0.0.1:8081/actuator/health/liveness` |
| Aggregate health | `GET http://127.0.0.1:8081/actuator/health` |
| Sentry | environment, release, exception type, stack trace, sanitized request 정보 |
| CloudWatch Logs | `requestId`, logger name, scheduler name, provider/outcome 키워드 |

CloudWatch 검색 키워드 예시:

```text
"[<request-id>]"
"MedicationAnalysisService"
"GoogleCalendarClient"
"ScheduleAlarmScheduler"
"MedicationAlarmScheduler"
"TodoAlarmScheduler"
"ReportAlarmScheduler"
"Gemini"
"web-push"
```

## 리포트 생성 실패

### 증상

- `POST /v1/medication-analysis/reports` 요청이 5xx로 실패한다.
- 리포트 snapshot은 생성됐지만 AI 결과가 비어 있거나 실패 상태다.
- 사용자가 리포트 생성은 요청했지만 결과 조회에서 기대한 리포트를 찾지 못한다.

### 확인 순서

1. Sentry에서 최근 500 이벤트를 확인한다.
2. stack trace의 실패 위치가 snapshot 생성, DB 저장, Gemini 호출, AI 응답 검증 중 어디인지 구분한다.
3. `requestId`를 확보해 CloudWatch Logs에서 같은 요청 흐름을 검색한다.
4. `MedicationAnalysisService`, `AnalysisEngine`, Gemini 관련 logger를 중심으로 전후 로그를 확인한다.
5. Actuator readiness로 DB 상태를 확인한다.
6. Gemini 관련 custom metric을 확인한다.

### 확인할 지표

| 지표 | 확인 목적 |
|------|-----------|
| `attune.gemini.requests{outcome=...}` | Gemini 성공/실패/quota 실패 증가 여부 |
| `attune.gemini.duration{outcome=...}` | Gemini 호출 지연 증가 여부 |
| Readiness | DB 장애가 리포트 저장 실패로 이어졌는지 확인 |

### 원인 분리

| 단서 | 가능한 원인 | 대응 |
|------|-------------|------|
| Gemini 429/quota | 외부 AI quota 또는 rate limit | 사용자에게 일시 실패 안내, quota 상태 확인 |
| AI JSON parsing/validator 실패 | Gemini 응답 형식 또는 금지 표현 검증 실패 | snapshot 보존 여부 확인, AI 결과만 실패 처리됐는지 확인 |
| DB/JPA 예외 | snapshot/report 저장 실패 | readiness, DB connection, transaction 로그 확인 |
| 최소 기록일/기간 검증 실패 | 정상적인 4xx 검증 실패 | Sentry 대상이 아니며 사용자 입력 안내 확인 |

## Google Calendar sync 실패

### 증상

- `POST /v1/calendar-connections/{connectionId}/sync` 요청이 실패한다.
- 외부 일정이 최신 상태로 갱신되지 않는다.
- 사용자에게 재연동 필요 또는 일시 장애 응답이 내려간다.

### 확인 순서

1. 사용자 응답이 재연동 필요(401/403, `invalid_grant`)인지 일시 장애(429/5xx/연결 실패)인지 확인한다.
2. Sentry 500 이벤트가 있으면 exception type과 stack trace를 확인한다.
3. `requestId`로 CloudWatch Logs를 검색한다.
4. `GoogleCalendarClient`, `CalendarConnectionService`, `CalendarEventService` 로그를 확인한다.
5. calendar custom metric에서 operation/outcome 분포를 확인한다.

### 확인할 지표

| 지표 | 확인 목적 |
|------|-----------|
| `attune.calendar.requests{operation=token,outcome=reauth}` | refresh token 만료/재연동 증가 |
| `attune.calendar.requests{operation=events,outcome=unavailable}` | Google API 일시 장애 또는 rate limit |
| `attune.calendar.requests{operation=calendar_list,outcome=failure}` | 캘린더 목록 조회 실패 |

### 원인 분리

| 단서 | 가능한 원인 | 대응 |
|------|-------------|------|
| `invalid_grant`, 401, 403 | 사용자 재연동 필요 | 연결 상태 안내, 재연동 유도 |
| 429 | Google rate limit | `Retry-After` 응답 여부 확인, 즉시 반복 호출 제한 |
| 5xx/timeout | Google 일시 장애 또는 네트워크 문제 | 일시 장애로 분류, 사용자 재시도 안내 |
| 중복 이벤트 | provider key upsert 문제 | `connectionId`, provider calendar/event id 기준 저장 로그 확인 |

Google API 오류 응답 body에는 일정 제목이나 계정 정보가 포함될 수 있으므로 원문 body를 로그, Sentry, 이슈에 복사하지 않는다.

## Web Push 알림 미발송

### 증상

- 복약, 일정, Todo, 리포트 알림이 예상 시각에 발송되지 않는다.
- 발송 이력이 `FAILED`, `SKIPPED`, `SENDING` 상태에 머문다.
- 특정 provider 또는 subscription에서 실패가 반복된다.

### 확인 순서

1. 알림 종류를 구분한다: medication, schedule, todo, report.
2. 해당 scheduler의 마지막 성공 시각을 확인한다.
3. CloudWatch Logs에서 scheduler 이름과 `"[no-request-id]"` 로그를 확인한다.
4. 발송 이력의 status, retry count, next 대상 여부를 확인한다.
5. push custom metric에서 provider/outcome 분포를 확인한다.
6. invalid subscription이면 구독 비활성화 처리가 됐는지 확인한다.

### 확인할 지표

| 지표 | 확인 목적 |
|------|-----------|
| `attune.scheduler.runs{scheduler=...,outcome=...}` | scheduler 실행 실패 증가 |
| `attune.scheduler.last.success{scheduler=...}` | scheduler 지연 또는 중단 |
| `attune.push.requests{provider=...,outcome=...}` | push provider 실패/invalid subscription 증가 |

### 원인 분리

| 단서 | 가능한 원인 | 대응 |
|------|-------------|------|
| `last.success`가 오래됨 | scheduler 미실행 또는 반복 실패 | 애플리케이션 상태, scheduler 로그 확인 |
| 실패 횟수 초과 | provider 실패 반복 | retry 정책과 terminal 실패 처리 확인 |
| invalid subscription 증가 | 만료되거나 폐기된 브라우저 구독 | subscription 비활성화 처리 확인 |
| recovery window 대상 누락 | 조회 조건 또는 시간대 문제 | due alarm 조회 조건, user zone 변환 확인 |

## Web Push 영수증 API 오류

### 증상

- `attune.notification.delivery.receipts{outcome=error}` 비율이 증가한다.
- provider 수락(`attune.push.requests{outcome=success}`) 대비 수신(`event=received`) 비율이 급감한다.
- 서비스 워커/프론트엔드에서 영수증 전송 실패 문의가 들어온다(단, 이 API는 fail-open이므로 알림 표시 자체는 영향받지 않아야 한다).

### 확인 순서

1. `attune.notification.delivery.receipts` 메트릭에서 `outcome` 분포를 확인한다. `invalid`/`expired`가 대부분이면 클라이언트(서비스 워커) 쪽 문제일 가능성이 높고, `error`가 대부분이면 서버/DB/Redis 문제일 가능성이 높다.
2. `error`가 높으면 Sentry에서 `NotificationDeliveryEventController`/`NotificationDeliveryReceiptService` 관련 500 이벤트를 확인한다.
3. `rate_limited` 비율이 높으면 특정 attempt id 또는 IP의 비정상 반복 호출인지 CloudWatch Logs에서 확인한다.
4. `expired` 비율이 높으면 배포 간 Web Push TTL(`notification.push.web-push.ttl-seconds`) 변경 여부, 또는 서비스 워커가 만료된 payload를 오래 붙들고 있는지 확인한다.
5. 이 API는 `permitAll`이므로 `requestId` 기반 CloudWatch Logs 검색과 함께 `NotificationDeliveryEventController` 로그를 확인한다.

### 확인할 지표

| 지표 | 확인 목적 |
|------|-----------|
| `attune.notification.delivery.receipts{event=...,outcome=...}` | event/outcome 조합별 증가 추이 |
| `attune.push.requests{outcome=success}` 대비 `receipts{event=received}` | provider 수락 대비 수신 급감 여부 |

### 원인 분리

| 단서 | 가능한 원인 | 대응 |
|------|-------------|------|
| `invalid` 급증 | 잘못된 attempt id/token, 서비스 워커 payload 파싱 오류, 오래된 payload 재전송 | 프론트 배포 버전, 구 payload 하위 호환 처리 확인 |
| `expired` 급증 | TTL 설정 변경, 클라이언트 시계 오차, 서비스 워커가 지연 처리 | `ttl-seconds` 설정값과 `receipt_expires_at` 계산 확인 |
| `rate_limited` 급증 | 특정 attempt/IP의 비정상 반복 호출, 또는 정상 트래픽 급증으로 한도가 낮음 | rate limit 설정값 재검토, 악성 트래픽이면 별도 차단 |
| `error` 급증 | DB/Redis 장애, 코드 예외 | readiness, Redis 연결 로그, Sentry stack trace 확인 |

Web Push 영수증 API는 attempt 존재 여부를 노출하지 않기 위해 모든 결과를 `204`로 응답한다. HTTP 상태만으로는 원인을 알 수 없으므로 반드시 위 메트릭으로 원인을 구분한다.

## Readiness 실패

### 증상

- 배포 health gate가 600초 안에 통과하지 못한다.
- `GET http://127.0.0.1:8081/actuator/health/readiness`가 503을 반환한다.
- 배포 후 컨테이너는 떠 있지만 트래픽 수신 준비가 되지 않는다.

### 확인 순서

1. 배포 워크플로 로그에서 readiness 실패 시점과 컨테이너 상태를 확인한다.
2. EC2 내부에서 readiness endpoint를 직접 확인한다.
3. aggregate health가 아니라 readiness의 `readinessState`, `db` 상태를 확인한다.
4. DB 연결 정보, Hikari 초기화, migration/DDL 오류 로그를 확인한다.
5. 컨테이너 exitCode와 최근 로그를 확인한다.

### 원인 분리

| 단서 | 가능한 원인 | 대응 |
|------|-------------|------|
| `db` down | MySQL 연결 실패 | DB host/port/credential, security group, secret 주입 여부 확인 |
| `readinessState` refusing traffic | 앱 초기화 미완료 | 부팅 로그와 Spring lifecycle 확인 |
| liveness는 성공, readiness만 실패 | JVM은 살아 있으나 DB 준비 안 됨 | DB/Hikari 중심으로 확인 |
| aggregate health만 실패 | Redis 등 비필수 의존성 장애 | 배포 게이트는 readiness 기준인지 확인 |

## Sentry 500 이벤트

### 확인 순서

1. environment, release, exception type을 확인한다.
2. stack trace 최상단의 애플리케이션 패키지(`attune.*`) 위치를 확인한다.
3. 이벤트에 query string, request body, cookie, 민감 header가 제거됐는지 확인한다.
4. `requestId`가 있으면 CloudWatch Logs에서 같은 요청을 검색한다.
5. 4xx 검증 실패가 ERROR로 잘못 기록되어 Sentry에 올라간 것은 아닌지 확인한다.

### 기록할 내용

- 발생 시각
- environment/release
- exception type
- 영향 API 또는 scheduler
- `requestId`
- 사용자 영향 범위
- 재현 가능 여부
- 조치 결과

### 기록하지 않을 내용

- JWT, refresh token, 인증 코드, 외부 API 키, 클라이언트 시크릿
- 이메일 주소, 이름 등 직접 식별 정보
- 증상/일지/복약 메모/AI 프롬프트/AI 응답 본문
- Google Calendar 오류 응답 body 원문
- push title/body 원문

## 빠른 체크리스트

- [ ] Sentry에서 500 이벤트와 stack trace를 확인했다.
- [ ] `requestId`를 확보해 CloudWatch Logs에서 요청 흐름을 확인했다.
- [ ] readiness/liveness로 앱 생존과 DB 준비 상태를 분리했다.
- [ ] 관련 custom metric으로 외부 API, scheduler, push 실패 증가 여부를 확인했다.
- [ ] 사용자에게 보여줄 메시지와 내부 원인을 분리했다.
- [ ] 민감정보를 로그, Sentry, 이슈, 문서에 옮기지 않았다.
