# 실행 계획: 운영 관측성 기반 구축

- 상태: active
- 작성자 / 날짜: Codex / 2026-06-26
- 관련 이슈/PR: 미정

## 목표

운영 장애를 사용자의 제보보다 먼저 탐지하고, 단일 요청·배포·외부 의존성 실패의 원인을 재현 가능한 로그·지표·추적으로 좁힐 수 있게 한다.

완료 시 운영자는 다음을 할 수 있어야 한다.

- 최근 배포 이후의 오류, 요청 지연, 컨테이너/JVM/DB 연결 상태를 한 화면에서 확인한다.
- API 5xx 급증, readiness 실패, 컨테이너 재시작, DB 연결 풀 고갈, 알림·Gemini 실패를 정해진 채널로 받는다.
- 요청 ID 또는 trace ID로 API 요청, 예외, Gemini·푸시·메일 같은 외부 호출을 연결해 확인한다.
- 민감한 건강·인증 데이터가 로그, 오류 추적, 메트릭 태그에 저장되지 않음을 테스트로 보장한다.

## 배경

현재 애플리케이션에는 Spring Boot Actuator와 콘솔/파일 로그가 있다. 그러나 지표가 프로세스 안에서만 유지되고 중앙 로그 수집·대시보드·알림·분산 추적은 없다. 운영 프로파일의 파일 로그(`/var/log/attune-me/app.log`)도 EC2에 종속된다.

또한 `/actuator/**`가 인증 없이 허용되고, DB와 diskspace health indicator가 비활성화되어 있다. 따라서 DB 장애 중에도 배포 health gate가 성공할 수 있으며, 운영 지표를 공개 인터넷에 노출할 위험이 있다.

## 현재 상태

- Actuator 노출: dev/prod는 management server `127.0.0.1:8081`에 바인딩한다. `/actuator/health/**`·`/actuator/info`만 허용하고 `/actuator/metrics` 등 나머지는 앱 레벨 `denyAll`. simple meter registry만 활성화.
- 배포: GitHub Actions가 EC2에서 host network Docker 컨테이너를 기동하고 `http://127.0.0.1:8081/actuator/health/readiness`를 최대 600초 폴링.
- 운영 로그: root `WARN`, `attune` `INFO`, 파일 롤링(10MB, 30일)과 콘솔 텍스트 로그.
- 데이터베이스: Hikari pool과 leak detection은 구성되어 있으나 운영 모니터링/알림은 없음.
- 외부·비동기 처리: Redis, MySQL, SMTP, web-push, Gemini, `@Async`, 분 단위 알림 scheduler가 존재.
- 예외: `GlobalExceptionHandler`가 5xx를 로그로 남기나 요청 상관관계 ID와 외부 오류 추적 연동은 없음.

## 변경 범위

### 권장 운영 스택

1. AWS CloudWatch Logs/Metrics/Alarm/Dashboard를 로그 수집과 인프라·애플리케이션 메트릭의 기본 저장소로 사용한다.
2. Sentry를 애플리케이션 예외와 제한된 성능 추적용으로 사용한다. `send-default-pii=false`를 기본으로 하고 요청·응답 본문 수집은 금지한다.
3. OpenTelemetry trace export(Tempo/Jaeger/OTel backend)는 Gemini·메일·푸시 병목 진단이 실제 필요해진 2단계 옵션으로 둔다. 초기에는 request ID를 모든 로그에 넣고 Sentry trace ID를 연계한다.

이 선택은 현재 단일 EC2 Docker 배포에서 Prometheus/Grafana/Loki를 별도 운영하는 부담을 피한다. CloudWatch·Sentry의 계정, 비용 한도, 보관 기간, Slack webhook/채널은 구현 전 승인한다.

### 애플리케이션

- Micrometer CloudWatch registry와 필요한 공통 계측을 추가한다.
- HTTP 요청에 UUID 기반 `requestId`를 생성하거나 전달받아 MDC에 넣는다. 응답에는 `X-Request-Id`를 반환한다.
- 로그 포맷을 JSON 구조화 로그로 바꾸고, 앱/환경/릴리스/requestId/traceId를 공통 필드로 남긴다.
- `Authorization`, JWT, cookie, 비밀번호, 이메일 인증 코드, 이메일 주소, 사용자 UUID, 증상·일기·복약·AI 프롬프트와 응답 본문은 로그·Sentry·지표 태그에서 제외한다. 예외 메시지도 기록 전에 해당 정책을 만족하는지 점검한다.
- Actuator는 health probe와 내부 metrics 수집에 필요한 endpoint만 노출하고 외부 공개를 차단한다. management port 혹은 EC2 loopback/private security group을 사용한다.
- liveness와 readiness를 분리한다. liveness는 JVM 생존만, readiness는 요청 처리 필수 의존성(DB 우선, Redis의 필수성은 토큰 캐시 장애 시 실제 동작을 검토 후 결정)을 검사한다.
- 배포 워크플로의 성공 판정을 readiness endpoint로 변경한다. 운영 health 상세 정보와 metrics endpoint는 외부 사용자에게 노출하지 않는다.
- Sentry DSN 등 새 비밀값은 `application-secret.yml`/GitHub Secret으로만 주입한다.

### 수집할 지표

- HTTP: 요청량, 상태 코드 계열, URI template 기준 p50/p95/p99 응답시간, in-flight 요청.
- JVM·컨테이너: heap/non-heap, GC pause, thread, CPU, 재시작 횟수, 디스크 사용량.
- DB/Hikari: active/idle/pending connection, timeout, connection acquisition 지연.
- Redis: 연결/명령 실패와 지연(사용 라이브러리에서 제공하는 범위 내).
- 비동기·스케줄러: executor queue/active/rejected, 각 scheduler의 시작·완료·실패·마지막 성공 시각.
- 외부 의존성: Gemini, SMTP, web-push 호출별 성공/실패/상태군/지연시간/재시도 횟수. 사용자 ID, subscription endpoint, 프롬프트, 수신자 주소는 태그로 사용하지 않는다.

### 알림과 대시보드

- API: 5xx 비율 2% 초과(5분), p95 1.5초 초과(10분)는 초기 알림 기준으로 적용 후 실제 기준선으로 조정한다.
- 가용성: readiness 실패, 컨테이너 재시작, 외부 synthetic health check 실패.
- 자원: CPU·메모리·디스크 80% 이상, Hikari pending 또는 pool 사용률 80% 이상.
- 업무: scheduler 마지막 성공 시각 초과, Gemini/메일/web-push 실패율 급증.
- 알림은 Slack 운영 채널로 보내며, 심각도·담당자·확인 절차·대시보드 링크를 포함한다.

## 제외 범위

- 사용자 행동 분석, 광고 분석, 건강 데이터 분석을 위한 제품 분석 플랫폼 도입.
- 로그/trace에 요청·응답 body를 저장하는 디버깅 기능.
- 다중 리전, 자동 확장, Kubernetes 전환.
- MySQL 서버 자체의 백업/복제 설계 변경.
- OpenTelemetry collector와 별도 trace backend 운영(2단계 후보).

## 관련 문서

- `docs/engineering/observability.md`
- `docs/quality/reliability.md`
- `docs/engineering/deployment-rules.md`
- `docs/engineering/ci-cd-rules.md`
- `docs/architecture/security-rules.md`
- `docs/agent/doc-gardening-workflow.md`

## 관련 코드

- `build.gradle` — Actuator만 의존, metrics exporter 없음
- `src/main/resources/application.yml` — Actuator 노출과 health indicator 설정
- `src/main/resources/application-prod.yml` — 운영 로그/Hikari 설정
- `src/main/java/attune/common/config/SecurityConfig.java` — `/actuator/**` 공개 규칙
- `src/main/java/attune/common/error/GlobalExceptionHandler.java` — 오류 로그 경로
- `src/main/java/attune/common/config/AsyncConfig.java` — 비동기 executor
- `.github/workflows/deploy-prod.yml`, `.github/workflows/deploy-dev.yml` — health gate와 Docker 실행

## 작업 단계

1. **인프라와 보안 결정 게이트**
   - AWS 계정/리전, CloudWatch log group 이름, 보관 기간(권장 30일), 월 비용 상한, 알림 Slack 채널을 확정한다.
   - EC2 instance profile에 CloudWatch Logs/Metrics 최소 권한만 부여한다. 장기 액세스 키는 컨테이너에 주입하지 않는다.
   - metrics/management 접근 경로를 확정한다. 권장안은 management port를 EC2 loopback 또는 private security group으로 제한하고, public ingress를 열지 않는 것이다.
   - Sentry 프로젝트, DSN, 보관·샘플링·PII 차단 정책을 승인한다.

2. **안전한 health와 Actuator 접근 제어**
   - `/actuator/**` 공개 허용을 제거하고 liveness/readiness 및 metrics endpoint의 접근 정책을 분리한다.
   - DB indicator를 readiness에 포함하고, diskspace도 다시 활성화한다. Redis는 인증 경로가 Redis 없이 동작 가능한지 확인한 뒤 readiness 포함 여부를 명시한다.
   - `management.endpoint.health.probes.enabled` 등을 프로파일별로 구성하고 배포 workflow가 readiness를 폴링하게 바꾼다.
   - health 응답이 의존성 상세·버전·시크릿을 노출하지 않는지 테스트한다.

3. **구조화 로그와 요청 상관관계**
   - servlet filter로 `X-Request-Id`를 수용 또는 생성하고 MDC 수명주기(비동기 전달과 정리 포함)를 구현한다.
   - logback JSON encoder를 적용하고 stdout을 기본 수집 경로로 한다. 파일 로그 유지 여부는 Docker log driver/CloudWatch 수집 완료 후 결정한다.
   - 공통 오류 로그 형식을 정하고, `GlobalExceptionHandler`와 외부 호출 로그에서 민감 정보가 남지 않게 정리한다.
   - request ID가 정상·4xx·5xx·비동기 로그 모두에 존재하는 테스트를 추가한다.

4. **메트릭과 도메인 계측**
   - CloudWatch registry와 공통 태그(`application`, `environment`, `release`)를 구성한다. 고카디널리티 태그(사용자 ID, raw URI, 예외 메시지)는 금지한다.
   - HTTP/JVM/Hikari 기본 meter가 전송되는지 확인한다.
   - Gemini·메일·web-push·scheduler·async executor의 성공/실패/지연/마지막 성공 지표를 작은 공통 계측 API로 추가한다.
   - 지표 이름, 단위, 태그, 소유 도메인을 `docs/engineering/observability.md`에 기록한다.

5. **오류 추적과 알림**
   - Sentry Spring integration을 `prod`에만 활성화하고 PII 차단, 환경/release 태그, 오류 전량·성공 trace 저샘플링을 적용한다.
   - CloudWatch dashboard와 Alarm을 구성한다. 알림마다 runbook과 dashboard link를 연결한다.
   - 외부 uptime monitor로 공개 health endpoint가 아니라 최소한의 사용자 경로 또는 별도 안전한 health probe를 점검한다.

6. **성능 기준선과 운영 검증**
   - 주요 시나리오(인증, 오늘 조회, 복약 기록, 일정 조회, AI 분석)를 k6 등으로 부하 테스트한다. 데이터는 비식별 테스트 계정만 쓴다.
   - 동시 사용자·목표 p95·허용 오류율을 기록하고, Hikari와 JVM 병목이 없는지 대시보드에서 검증한다.
   - 인위적으로 5xx, DB 연결 실패, Gemini 429/timeout, scheduler 실패를 발생시켜 로그→지표→알림→runbook 흐름을 확인한다.
   - 초기 2주간 알림을 관찰해 임계값과 샘플링을 조정한다.

## 검증 방법

- `scripts/agent/verify` 통과.
- Spring Boot integration test로 liveness/readiness 상태와 공개 endpoint 차단 여부를 검증.
- MockWebServer 또는 mock을 사용해 Gemini/푸시/메일 실패 시 counter·timer·오류 로그가 발생하는지 검증.
- request ID filter의 정상·오류·비동기 MDC 전파 테스트.
- production-like dev 환경에서 CloudWatch Logs에 JSON 로그, CloudWatch Metrics에 필수 meter, Sentry에 비PII 오류가 도착하는지 수동 검증.
- Docker 재시작, DB 연결 차단, readiness 실패에서 배포 workflow가 올바르게 실패하고 기존 컨테이너 롤백 절차가 가능한지 확인.
- 로그·Sentry event·metric tag를 표본 검토하여 JWT, 이메일, 사용자 UUID, 증상/일기/복약/AI 본문이 없는지 확인.

## 위험 요소

| 위험 | 대응 |
|---|---|
| Actuator 공개로 운영 정보 노출 | management endpoint를 private path/port와 network rule로 제한하고 접근 테스트를 추가한다. |
| readiness가 외부 의존성에 과도하게 묶여 불필요한 배포 실패 | liveness와 readiness를 구분하고, Redis·SMTP·Gemini의 필수성은 장애 시 제품 동작 기준으로 결정한다. |
| 로그에 건강·인증 데이터 유출 | body logging 금지, 허용 필드 방식, redaction 테스트, Sentry PII 전송 차단을 적용한다. |
| high-cardinality metric과 trace/log 비용 폭증 | 고카디널리티 태그 금지, 보관 기간·월 비용 상한·sampling을 설정한다. |
| 단일 EC2에서 수집 agent 장애 | agent 상태와 마지막 수집 시각을 별도 감시하고, 컨테이너 stdout을 기본 로그 원본으로 유지한다. |
| 관측성 코드가 정상 요청 성능을 악화 | 비동기 전송·낮은 trace sampling, 부하 기준선 비교, exporter 장애가 요청을 막지 않도록 구성한다. |

## 롤백 방법

1. 애플리케이션 endpoint/health 변경으로 배포가 실패하면 직전 이미지로 되돌리고, 배포 gate를 기존 `/actuator/health`로 한시 복구한다.
2. metrics exporter 또는 Sentry 전송 문제가 요청 성능에 영향을 주면 해당 profile의 exporter/DSN을 비활성화하고 로그와 기본 Actuator만 유지한다.
3. JSON 로그 파서 문제가 있으면 console formatter만 기존 text pattern으로 되돌린다. 민감정보 정책과 request ID filter는 유지한다.
4. CloudWatch/Sentry 리소스는 코드 롤백 뒤에도 보존해 원인 분석에 사용하고, 비용 문제일 때만 보관 정책을 별도로 변경한다.

## 의사결정 로그

- 2026-06-26: 초기 저장소는 AWS CloudWatch, 오류 추적은 Sentry를 권장한다. 현재 단일 EC2 Docker 규모에서 자체 Prometheus/Grafana/Loki 운영보다 인프라 부담이 낮다.
- 2026-06-26: 요청 본문·AI 입력/출력·건강 데이터는 로그와 tracing의 제외 대상이다. 운영 편의보다 개인정보 보호가 우선이다.
- 2026-06-26: DB는 readiness에 포함한다. 현재 DB indicator 비활성 상태는 실제 요청 처리 가능 여부를 배포 gate가 반영하지 못한다.
- 2026-06-27: 1차 구현으로 `X-Request-Id` 필터, 로그 MDC 주입, 비동기 executor MDC 전파, 콘솔 로그 requestId 패턴을 추가했다. Actuator 접근 제어와 CloudWatch/Sentry 연동은 운영 접근 정책·비용·비밀값 결정 후 진행한다.
- 2026-06-27: dev 부하 테스트용 `loadtest` 프로파일을 추가했다. `dev,loadtest` 조합에서 DB·diskspace health와 liveness/readiness probe를 켜고, 로그 레벨을 낮추며, web-push는 stub으로 전환한다. 메일·Gemini·scheduler 차단은 현재 설정만으로는 불가능하므로 별도 코드 변경 후보로 둔다.
- 2026-06-27: Step 2(health probe·actuator 접근 제어) 구현. liveness/readiness probe를 base(`application.yml`)에서 활성화하고, `db`·`diskspace` health indicator를 켰다. readiness 그룹은 `readinessState + db`로 한정해 Redis/메일 등 비필수 의존성과의 과결합을 피한다(Redis health는 aggregate `/actuator/health`에만 포함). 배포 게이트(`deploy-prod`/`deploy-dev`)는 aggregate 대신 `/actuator/health/readiness`를 폴링하도록 변경했다. SecurityConfig에서 `/actuator/health/**`·`/actuator/info`만 공개하고 `/actuator/metrics` 등 나머지는 `denyAll`로 차단했다. 메트릭은 in-process Micrometer로만 수집하므로 HTTP 차단이 무방하다. management port/네트워크 레벨 제한은 운영 접근정책 확정 후 후속.
- 2026-06-27: Step 3(민감정보 로그) 1차. 공지 메일 발송 실패 로그가 `email` 주소를 남기던 것을 `userId`로 교체했다. 로그/Sentry/메트릭의 PII 금지 정책을 `observability.md`에 명문화했다. 로그의 `userId`(UUID) 사용은 디버깅 식별자로 허용하되 메트릭 태그로는 쓰지 않는다.
- 2026-06-28: Step 3 심화 1차 완료. `StubPushSender`와 `AdminNotificationSender`는 push title/body 본문 대신 길이만 로그에 남기도록 변경했다. `GoogleCalendarClient`와 `GeminiTextGenerator`는 외부 API response body를 로그 메시지와 예외 cause에 보존하지 않도록 변경했다. 문의 메일 실패 로그와 Google OAuth 검증 예외 로그는 원본 예외/메시지 대신 에러 타입만 남긴다. 외부 API response body와 push 본문 금지 정책을 `observability.md`에 추가했다.
- 2026-06-29: Step 2 후속. dev/prod management server를 public 앱 포트와 분리해 기본 `127.0.0.1:8081`에 바인딩한다. deploy-dev/deploy-prod readiness gate는 EC2 내부 `http://127.0.0.1:8081/actuator/health/readiness`를 폴링한다. local 프로파일은 기존 단일 포트 동작을 유지한다.
- 2026-06-29: 운영 정책 결정. AWS 리전은 `ap-northeast-2`(EC2 AZ `ap-northeast-2c`), 월 비용 상한은 10,000 KRW 이하, CloudWatch Logs 보관은 7일 시작, custom metrics/alarms는 각각 10개 이하로 시작한다. Sentry는 Free/Developer 범위에서 오류 이벤트 중심으로 쓰고 trace/log/replay/profiling은 비활성 또는 최소 샘플링으로 둔다. Slack 운영 채널은 `#attune-prod`로 한다.
- TODO(owner, YYYY-MM-DD, reason): EC2 IAM instance profile 적용 담당자/시점과 Sentry DSN 주입 방식을 확정한다.

## 완료 조건

- [ ] 중앙 로그에서 request ID로 API 오류를 검색할 수 있다. 애플리케이션 로그의 requestId 주입은 완료했으며, 중앙 로그 수집 연동은 남아 있다.
- [ ] 운영 dashboard에서 HTTP, JVM/컨테이너, Hikari, 외부 의존성, scheduler 상태를 볼 수 있다. 비용 제한형 1차 metric/alarm 카탈로그는 `observability.md`에 기록했다.
- [x] readiness는 DB 장애를 실패로 보고, liveness는 외부 의존성 장애에 의해 불필요하게 실패하지 않는다. (readiness=`readinessState+db`, 배포 게이트가 readiness 폴링)
- [x] Actuator metrics와 health 상세는 public internet에서 접근할 수 없다. (dev/prod management server=`127.0.0.1:8081`, metrics는 앱 레벨 `denyAll`) `ActuatorSecurityTest`와 `ManagementServerProfileConfigTest`로 검증.
- [ ] 정의된 장애 조건이 Slack 알림과 runbook으로 연결된다.
- [ ] Sentry와 로그/메트릭 표본에 개인정보·JWT·요청 본문이 없다.
- [ ] 부하 기준선과 알림 임계값 조정 기록이 문서화되어 있다.
- [ ] `scripts/agent/verify`가 통과한다.

## 작업 후 문서 업데이트 목록

- [ ] `docs/engineering/observability.md` (endpoint, 로그 필드, meter, dashboard/alert/runbook 링크)
- [ ] `docs/quality/reliability.md` (liveness/readiness와 장애 대응)
- [ ] `docs/engineering/deployment-rules.md` (readiness gate, rollback)
- [ ] `docs/architecture/security-rules.md` (management endpoint 접근 및 telemetry 개인정보 정책)
- [ ] `docs/architecture/system-overview.md` (운영 관측성 구성)
- [ ] `docs/notes.md` 또는 `docs/exec-plans/tech-debt-tracker.md` (OpenTelemetry 2단계 후보)
