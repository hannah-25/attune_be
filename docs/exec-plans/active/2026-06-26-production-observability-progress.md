# 진행 상황: 운영 관측성 기반 구축

- 관련 계획: [`2026-06-26-production-observability.md`](./2026-06-26-production-observability.md)
- 관련 PR: [#85](https://github.com/hannah-25/attune_be/pull/85) (`feature/production-observability-health-probes` → `develop`, MERGED)
- 최종 갱신: 2026-06-28

> 계획서의 단계(Step 1~6)에 대한 **현재 진척**과 **남은 작업**을 한눈에 본다.
> 상세 설계·위험·롤백은 계획서를, 결정 이력은 계획서의 "의사결정 로그"를 참고.

## 한눈에 보기

| 단계 | 내용 | 상태 |
|------|------|------|
| Step 1 | 인프라·보안 결정 게이트 (AWS/Sentry/Slack/IAM) | ⛔ 외부 결정 대기 |
| Step 2 | 안전한 health / Actuator 접근 제어 | ✅ 앱 레벨 완료 (네트워크 레벨 후속) |
| Step 3 | 구조화 로그·요청 상관관계 | 🟡 requestId·외부 호출 PII 감사 1차 완료, JSON 로그 후속 |
| Step 4 | 메트릭·도메인 계측 (CloudWatch) | ⛔ Step 1 결정 후 |
| Step 5 | 오류 추적·알림 (Sentry/대시보드) | ⛔ Step 1 결정 후 |
| Step 6 | 성능 기준선·운영 검증 (부하 테스트) | 🟡 loadtest 프로파일 준비됨 |

---

## 완료한 작업 (PR #85)

PR #85는 **외부 계정 결정이 필요 없는 코드/설정 범위**만 다룬 1차 구현이다. 커밋 3개.

### 1. `refactor:` MedicationService 정리
- `getCurrentUserId()` 헬퍼를 `SecurityUtils.getCurrentUserUuid()` 직접 호출로 인라인화. 동작 변화 없음(관측성과 무관하여 분리 커밋).

### 2. `feat:` 운영 관측성 1차

**요청 상관관계 (Step 3 일부)**
- `RequestIdFilter`: `X-Request-Id` 수용/생성(길이·패턴 검증) → MDC 주입, 응답 헤더 반환. ERROR 디스패치 재진입 시 동일 ID 재사용.
- `AsyncConfig`: `@Async` executor에 MDC 전파 `TaskDecorator` (비동기 로그도 같은 requestId로 추적).
- 콘솔/파일 로그 패턴에 `[requestId]` 추가, `X-Request-Id`를 CORS 노출 헤더로 추가.

**Health probe / Actuator 접근 제어 (Step 2)**
- liveness/readiness probe 활성화, `db`·`diskspace` health indicator on.
- **readiness = `readinessState + db`** — Redis/메일 등 비필수 의존성과의 과결합 방지 (Redis는 aggregate `/actuator/health`에만 포함).
- 배포 게이트(`deploy-prod`/`deploy-dev`)를 `/actuator/health/readiness` 폴링으로 변경 → DB 장애는 배포 실패로 반영하되 Redis 일시 장애로는 안 깨짐.
- `/actuator/health/**`·`/actuator/info`만 공개, `/actuator/metrics` 등 나머지 `denyAll` (메트릭은 in-process Micrometer 수집).
- 부하 테스트용 `loadtest` 프로파일 추가.

**민감정보 로그 (Step 3 일부)**
- 공지 메일 발송 실패 로그의 `email` 주소 → `userId`로 교체.
- PII 로깅 금지 정책을 `docs/engineering/observability.md`에 명문화.

**테스트**
- `RequestIdFilterTest`, `AsyncConfigTest`, `ActuatorSecurityTest`.

### 3. `chore:` PR 리뷰 반영 (gemini-code-assist)
- ERROR 디스패치에서도 requestId 유지 (`shouldNotFilterErrorDispatch()=false` + request attribute 재사용).
- `application-prod.yml`의 `logging.pattern.file`에도 requestId 추가.

> `scripts/agent/verify` 전체 통과(빌드+테스트+ArchUnit+시크릿/문서).

---

## 남은 작업

### A. PR #85 마무리
- [x] 사람 리뷰 승인 → **머지** (현재 MERGED)

### B. 외부 결정이 필요한 항목 — ⛔ 진행 막힘 (Step 1)
사람이 정해줘야 후속(C)을 진행할 수 있다. 계획서 TODO와 동일.
- [ ] AWS 계정/리전, CloudWatch log group·보관기간(권장 30일), **월 비용 상한**
- [ ] Sentry 프로젝트/DSN/샘플링·PII 차단 정책
- [ ] 알림용 **Slack 운영 채널**, EC2 IAM 적용 담당자

### C. 결정 후 구현할 단계 (Step 4·5·6)
- [ ] **Step 4 메트릭**: Micrometer CloudWatch registry, 공통 태그(`application`/`environment`/`release`), HTTP/JVM/Hikari + Gemini·메일·push·scheduler 성공/실패/지연/마지막 성공 계측. 지표 카탈로그를 `observability.md`에 기록.
- [ ] **Step 5 오류추적·알림**: Sentry Spring integration(prod 한정, PII 차단, 환경/release 태그), CloudWatch 대시보드·Alarm + 알림마다 runbook·대시보드 링크.
- [ ] **Step 6 성능 기준선**: k6 부하 테스트(이미 `loadtest` 프로파일 준비), 동시 사용자·p95·오류율 기록, 장애 주입 검증(5xx/DB 차단/Gemini 429·timeout/scheduler 실패), 초기 2주 임계값·샘플링 조정.

### D. 후속 보강 — 외부 결정 불필요 (코드/설정)
- [ ] management port/네트워크 레벨 actuator 접근 제한 (현재는 앱 레벨 `denyAll`만).
- [x] Step 3 심화 1차: 외부 호출(Gemini/메일/push/calendar) 로그 PII 점검.
  - `StubPushSender`는 title/body 본문 대신 길이만 로그에 남기도록 변경하고 회귀 테스트 추가.
  - `AdminNotificationSender`는 push title 본문 대신 길이만 로그에 남기도록 변경.
  - `GoogleCalendarClient`는 Google API response body를 로그와 예외 cause에 남기지 않도록 정리하고 회귀 테스트 추가.
  - `GeminiTextGenerator`는 Gemini HTTP 오류 body를 예외 cause에 보존하지 않도록 정리하고 회귀 테스트 추가.
  - 문의 메일 실패 로그와 Google OAuth 검증 예외 로그는 원본 예외/메시지 대신 에러 타입만 남기도록 변경.
- [ ] 구조화 JSON 로그 전환 (현재는 텍스트 패턴 + requestId). 중앙 로그 수집 연동 시점에 맞춰 결정.

---

## 진행 시 주의 (AGENTS.md §8)

B·C·D 일부는 **인증/인가·배포 워크플로·외부 연동·비용 발생**에 해당한다.
[AGENTS.md §8](../../../AGENTS.md#8-확실하지-않을-때-사람에게-확인할-기준)에 따라 **실행 전 사람에게 확인**한 뒤 진행한다.
