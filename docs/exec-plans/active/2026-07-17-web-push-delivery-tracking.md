# 실행 계획: Web Push 도달 상태 추적

- 상태: active
- 작성일: 2026-07-17
- 관련 이슈/PR: 미정

## 목표

Web Push 공급자 수락(`SENT`), 서비스 워커 수신(`RECEIVED`), 알림 표시 요청 성공(`DISPLAYED`), push 클릭으로 앱을 연 상태(`OPENED`)를 구분해 기록한다. push가 유실돼도 사용자는 앱 내 알림함에서 미확인 알림을 확인할 수 있어야 한다.

Web Push는 실제 OS 표시나 사용자의 인지를 보장하지 않는다. `OPENED`도 읽음이 아니므로, 알림함의 읽음 상태는 별도 `readAt`으로 관리한다.

## 현재 상태

- `NotificationHistory.status=SENT`는 `WebPushSender`가 provider에서 HTTP 2xx를 받은 상태다.
- 이력은 사용자·알람 단위라서 여러 브라우저/기기의 결과를 구분하지 못한다.
- payload는 `title`, `body`, `url`뿐이고, 서비스 워커 영수증 API와 알림함 API가 없다.
- 수신 API를 새로 만들 경우 현 `SecurityConfig`는 JWT 없이는 요청을 거부한다.

## 변경 범위

### 사전 승인 게이트

- `notification_deliveries`, `notification_delivery_attempts`, `notification_history.url`, `notification_history.read_at`은 현재 `docs/db_schema.md`에 없는 스키마 변경이다. 프로젝트 데이터 규칙에 따라 **구현·migration 작성 전에 담당자의 스키마 변경 승인을 받는다.** 승인 후에만 스키마 문서와 SQL migration을 확정한다.

### 데이터 모델과 migration

1. `notification_deliveries` / `NotificationDelivery`를 추가한다. 한 `NotificationHistory`를 한 활성 구독에 보내는 전체 단위다.
   - `id` UUID, `notification_history_id`, `subscription_id`, `created_at`, `updated_at`.
   - `provider_accepted_at`, `received_at`, `displayed_at`, `opened_at`은 하위 attempt의 최초 시각을 요약한 값이다.
   - `UNIQUE(notification_history_id, subscription_id)`로 같은 구독의 중복 delivery 생성을 막는다.
2. `notification_delivery_attempts` / `NotificationDeliveryAttempt`를 추가한다. 실제 외부 전송 시도마다 한 행을 만든다.
   - `id` UUID, `delivery_id`, `attempt_no`, `receipt_token_hash`, `receipt_expires_at`, `provider_accepted_at`, `received_at`, `displayed_at`, `opened_at`, `failed_at`, `failure_reason`, `created_at`.
   - `UNIQUE(delivery_id, attempt_no)`를 둔다.
   - receipt token은 CSPRNG로 생성한 최소 256-bit 값을 base64url로 인코딩하고 SHA-256 해시만 저장한다. endpoint, payload 원문, 토큰 평문, 원문 예외는 저장하지 않는다.
   - `receipt_expires_at`은 발송 시 명시적으로 지정하는 Web Push TTL과 서버 시계 오차를 포함한 수락 창의 끝이다. 현재 `WebPushSender.java`/`WebPushConfig.java`는 TTL을 넘기지 않고 라이브러리 기본값에 의존하고 있어 기준으로 삼을 값이 없으므로, 발송 로직에 명시적 TTL 설정을 추가하는 작업을 이번 범위에 포함한다(아래 "발송과 재시도" 3번). 지연 도착을 위한 과거 attempt의 hash는 보존하되, 만료된 token으로는 새 event를 기록하지 않는다. delivery/attempt와 hash의 보존 기간은 개인정보 보존 정책과 함께 migration 승인 시 확정한다.
3. `notification_history`에 nullable `url`과 nullable `read_at`을 추가한다.
   - `url`은 알림함 이동 경로를 보존한다. 기존 행은 `/home`으로 해석한다.
   - `read_at`은 인증된 사용자가 알림함에서 읽음 처리한 최초 시각이다. `OPENED`와 혼용하지 않는다.
4. `notification_deliveries`·`notification_delivery_attempts`의 모든 FK(`notification_history_id`, `subscription_id`, `delivery_id`)에 `ON DELETE CASCADE`를 건다. 이와 별개로 `UserDataDeletionExecutor.DELETE_STATEMENTS`에 두 테이블의 삭제문을 `notification_history`/`notification_subscriptions`보다 앞에 명시적으로 추가한다(둘 다 leaf 테이블이라 리스트의 나머지 순서가 바뀌어도 안전). 테스트 스키마(`ddl-auto=create-drop`)는 이 두 테이블도 기존 `NotificationHistory`/`NotificationSubscription`처럼 순수 `@Column` FK라 Hibernate가 FK 제약 자체를 생성하지 않으므로, cascade는 이 executor를 거치지 않는 경로에 대한 방어선이고 명시적 삭제문이 테스트로 검증되는 1차 보장 수단이다. 일반 재구독은 in-place 갱신이라 ID가 유지되는지 확인한다.
5. `docs/db_schema.md`와 `docs/sql/` migration을 먼저 갱신한다. 운영은 `ddl-auto=validate`이므로 migration을 배포 전에 적용한다.

### 발송과 재시도

1. `NotificationService`는 활성 구독별 `NotificationDelivery`를 만들고, 매 외부 발송 직전에 새 `NotificationDeliveryAttempt`와 receipt token을 생성한다.
2. 현재 TX1(이력 선점·즉시 커밋) → 트랜잭션 밖 Web Push 호출 → TX2(결과 반영) 경계를 유지한다.
   - delivery/attempt 생성과 token hash 저장은 payload를 만들기 전인 TX1에 편입한다.
   - provider 결과는 TX2에서 attempt에 반영하고 delivery의 최초 상태를 집계한다.
3. `PushSender`, `PushSenderRouter`, `WebPushSender`가 구독·attempt별 payload를 받도록 바꾼다. `WebPushSender`는 발송마다 명시적 TTL(초)을 `pushService.preparePost(...)`에 지정하도록 바꾼다 — 현재는 TTL 인자 없이 라이브러리 기본값에 의존하고 있어 `receipt_expires_at` 계산의 기준이 없다.

```json
{
  "title": "복약 시간",
  "body": "복약 시간이 됐어요.",
  "url": "/medication",
  "deliveryAttemptId": "uuid",
  "receiptToken": "opaque-random-token"
}
```

4. provider 2xx는 해당 attempt의 `providerAcceptedAt`으로 기록한다. 기존 `NotificationHistory.SENT`의 의미는 “최소 한 활성 구독이 provider에 수락됨”으로 유지한다.
5. 404/410은 attempt 실패를 남기고 기존처럼 구독을 비활성화한다. delivery는 `(notification_history_id, subscription_id)` 기준으로 **find-or-create**한다. 기존 delivery가 있으면 **새 attempt와 새 token**만 추가한다. `reclaimAndLoadSubscriptions`(`NotificationTxOperations.java`)는 재시도 시 활성 구독을 다시 조회하므로, 최초 발송 이후 새로 추가된 구독처럼 기존 delivery가 없는 경우는 새 delivery와 첫 attempt를 함께 만든다. 먼저 보낸 payload가 늦게 도착할 수 있으므로 이전 attempt와 token hash를 덮어쓰거나 무효화하지 않는다. 단, 영수증 API는 각 attempt의 `receipt_expires_at` 전까지만 token을 수락한다.
6. 기존 중복 방지, 일정 알림 재시도, 복약·할 일 후보 조회를 회귀시키지 않는다.

### 영수증 API와 보안

1. 서비스 워커용 API를 추가한다.

```http
POST /v1/notification-delivery-attempts/{deliveryAttemptId}/events
Content-Type: application/json

{
  "event": "RECEIVED",
  "receiptToken": "opaque-random-token"
}
```

2. `SecurityConfig`에서 위 POST 경로만 `permitAll()`로 허용한다. JWT 인증을 우회하는 대신 controller/service가 attempt ID, 만료되지 않은 receipt token hash, 허용 event를 검증하고 hash 비교는 상수 시간 비교로 수행한다.
3. 허용 event는 `RECEIVED`, `DISPLAYED`, `OPENED`다. 중복 요청은 성공으로 처리하고 최초 시각을 바꾸지 않는다. 후행 event가 먼저 도착하면 비어 있는 선행 단계를 같은 시각으로 backfill하며, 해당 값이 추정치임을 지표 정의에 명시한다.
4. token은 URL path/query에 넣지 않고 body로만 보낸다. 로그·예외·metric tag에서 token, endpoint, 제목·본문을 제외한다.
5. CORS allowlist, request 형식 검증, attempt/token 및 IP/글로벌 단위 rate limit을 적용한다. 정상·중복·존재하지 않음·token 불일치·만료 요청 모두 `204 No Content`로 응답해 attempt 존재 여부를 노출하지 않는다. 서버 내부에는 결과를 `accepted`, `duplicate`, `invalid`, `expired`, `rate_limited`, `error`로 구분해 기록한다.

### 알림함과 프론트엔드

1. 인증된 알림함 API를 추가한다.
   - `GET /v1/notifications?cursor=...&status=UNREAD|READ`
   - `PATCH /v1/notifications/{notificationHistoryId}/read`
   - 목록의 기본 정렬은 `sentAt DESC, id DESC`이고 cursor는 마지막 항목의 `(sentAt, id)`를 불투명하게 인코딩한다. 다음 페이지는 이 복합 키보다 이전인 행만 조회해 동률·새 발송이 있어도 중복/누락 없이 이어진다.
   - cursor 비교(`(sentAt, id) < (cursorSentAt, cursorId)`)는 JPQL이 지원하지 않는 SQL row-value 문법이므로, `sentAt < :cursorSentAt OR (sentAt = :cursorSentAt AND id < :cursorId)`로 풀어 쓰거나 native query로 구현한다.
   - 이 API만 기존 목록 API(`NoticeService`, `CommunityService` 등)의 `Pageable`/`Page<T>` 컨벤션 대신 cursor 방식을 쓴다. 알림함은 계속 새 알림이 쌓이는 목록이라 offset 방식이 중복/누락을 일으키기 쉬워 의도적으로 다르게 간다.
   - `status=UNREAD|READ`는 각각 `readAt IS NULL` / `readAt IS NOT NULL`이다. migration에 `(user_id, read_at, sent_at, id)` 인덱스를 추가해 사용자별 상태 필터와 cursor 조회를 지원한다.
   - 목록은 하나의 `NotificationHistory`를 한 알림으로 표시하고 여러 delivery/attempt 중 가장 진전된 전송 상태를 요약한다.
   - `OPENED`는 클릭 상태로만 반환하고, 읽음은 `readAt`으로 반환한다.
2. 프론트 서비스 워커를 수정한다.
   - `push` payload를 파싱한 뒤 `RECEIVED`를 보낸다. 영수증 fetch는 짧은 timeout을 적용하고 실패를 삼켜 알림 표시를 막지 않는다.
   - `showNotification()` 완료 뒤 `DISPLAYED`를 보낸다. `showNotification()`과 receipt fetch는 서로 의존시키지 않는다.
   - `notificationclick`에서 `OPENED`를 보내고 payload URL로 이동한다. 영수증 전송 실패가 앱 창 열기/포커스를 막아서는 안 된다.
   - 각 fetch·알림 표시·클릭 처리는 `event.waitUntil()` promise에 포함하되, 병렬 작업은 `Promise.allSettled()`로 묶는다. 영수증 성공 여부와 무관하게 `showNotification()` 및 창 열기/포커스가 실행되는 fail-open 동작을 보장한다.
   - 구 버전 payload는 계속 표시하고 영수증만 생략한다.
3. 알림함 UI는 push 수신 여부와 관계없이 미확인 알림을 보여 주고, 사용자가 열면 읽음 처리한다. 권한·구독 상태 안내와 재구독 흐름을 제공한다.
4. payload를 직렬화한 뒤 byte 크기를 측정하고 초기 안전 한도 3 KB를 적용한다. 초과하면 제목/본문을 절단하고, 상세 내용은 알림함 API로 조회한다. URL은 내부 상대 경로만 허용한다.

### 관측성 및 운영

1. 기존 `attune.push.requests`와 별도로 `attune.notification.delivery.receipts{event=received|displayed|opened, outcome=accepted|duplicate|invalid|expired|rate_limited|error}`를 기록한다. receipt API 오류 증가는 이 metric의 `outcome`으로 판별한다.
2. provider 수락·수신·표시·클릭·읽음 비율을 분리해 정의한다. receipt 없는 `SENT`를 유실로 단정하지 않는다.
3. `docs/engineering/observability.md`의 1차 custom metric 카탈로그(`observability.md:18`, 메트릭 이름 기준 10개 이하)에 새 metric을 추가한다. 현재 카탈로그는 7개이고 이번 추가로 8개가 되어 예산 안에 있다. 이 상한은 시계열(태그 조합) 수가 아니라 메트릭 이름 개수 기준임을 명확히 한다.
4. 운영 대시보드와 incident runbook에 provider 수락 급감, 수락 대비 수신 급감, 만료 구독 증가, receipt API 오류 증가를 추가한다.
5. 초기 2주간 브라우저별 수신율, receipt API 오류율, payload 크기, service worker 호환성을 관측한다.

## 제외 범위

- Android/iOS 네이티브 FCM/APNS 발송 구현.
- 실제 기기 표시 또는 사용자의 인지를 보장하는 기능.
- 기존 발송 이력의 수신 상태 소급 변환.

## 작업 단계

각 단계는 별도 feature 브랜치 → PR로 진행한다(`develop` 직접 push 금지). 문서 갱신은 마지막으로 몰지 않고 해당 변경을 만든 PR에 함께 포함한다.

### 0. 게이트 — 코드 작업 시작 전 필수, 이후 전 단계를 막는다

1. 새 테이블·컬럼, token/attempt 보존 기간, 알림함 정렬·cursor·인덱스에 대해 담당자의 스키마 변경 승인을 받는다.
2. 프론트 저장소의 서비스 워커·인증·알림 UI를 조사하고 API 계약(요청/응답 필드, 인증 방식, 알림함 UX)을 확정한다. PR1의 DTO/스키마 설계에 반영해야 하므로 승인 절차와 병행한다.

### PR 1 — DB 스키마 (다른 모든 백엔드 작업의 전제)

- `docs/db_schema.md`와 `docs/sql/` migration: `notification_deliveries`, `notification_delivery_attempts`, `notification_history.url`/`read_at`, 모든 FK에 `ON DELETE CASCADE`, `(user_id, read_at, sent_at, id)` 인덱스.
- JPA 엔티티 추가/수정.
- 사용자 탈퇴 삭제 경로: cascade로 해결되는지 Testcontainers로 검증한다(`UserDataDeletionExecutor.java` 자체는 변경이 없을 수 있다).
- DB 통합 테스트: delivery/attempt unique 제약, cascade delete.

### PR 2 — 발송 경로 (PR1에 의존)

- `WebPushSender`에 명시적 TTL 지정.
- `NotificationTxOperations`: 구독별 delivery/attempt find-or-create와 token 생성을 TX1(`claimAndLoadSubscriptions`/`reclaimAndLoadSubscriptions`)에 편입.
- `PushSender`/`PushSenderRouter`/`WebPushSender` 시그니처를 attempt별 payload(`deliveryAttemptId`, `receiptToken`)로 변경.
- 기존 발송·재시도·중복 방지 회귀 테스트가 계속 통과하는지 확인.

### PR 3 — 영수증 API와 보안 (PR2에 의존, PR4와 병렬 가능)

- `POST /v1/notification-delivery-attempts/{id}/events`, `SecurityConfig`에 `HttpMethod.POST`로 스코프한 `permitAll` 추가.
- attempt ID·token 검증(상수 시간 비교), 만료 체크, 멱등/backfill.
- rate limiter 신규 구현(기존 rate limiting 인프라 없음 — Redis 재사용).
- `attune.notification.delivery.receipts` 메트릭, `observability.md`/`incident-runbook.md` 갱신.
- 보안/멱등 통합 테스트.

### PR 4 — 알림함 API (PR1에 의존, PR3와 병렬 가능)

- `GET /v1/notifications`(cursor — native query 또는 OR 전개로 구현), `PATCH /v1/notifications/{id}/read`.
- DTO, `api-guide.md` 갱신.
- cursor pagination·인덱스 사용 통합 테스트.

### PR 5 — 프론트엔드 서비스 워커 (PR3·PR4 배포 후)

- `push`/`showNotification`/`notificationclick`에 fail-open `event.waitUntil()` 영수증 전송.
- 알림함 UI, 구 payload 하위 호환.
- 프론트 저장소의 서비스 워커/알림함 사용자 문서 갱신.

### PR 6 — 통합 검증과 점진 배포

- 지원 브라우저에서 frontend-backend 통합 테스트, feature flag 기반 점진 배포.

## 검증 방법

- `./gradlew test`와 `scripts/agent/verify`.
- Testcontainers: delivery/attempt unique 제약, 재시도 전후 각 token 유효성, 만료 전후 token 수락/거부, 중복/역순 receipt 멱등성, token 불일치, 타 사용자 읽음 처리 차단, 사용자 탈퇴 삭제, read 상태별 cursor pagination 및 인덱스 사용 계획.
- Web Push: 사용자 한 명의 구독 두 개, provider 2xx·404/410·일시 실패·재시도 뒤 이전 attempt의 지연 수신을 검증.
- 서비스 워커: `waitUntil()` 안에서 `RECEIVED`/`DISPLAYED`/`OPENED`가 전송되는지, 영수증 API 오류·timeout에도 알림 표시와 창 열기/포커스가 실행되는지, 오프라인·권한 거부·종료·구 payload를 검증.
- payload: 한글/이모지/긴 공지 본문에서 byte 한도와 절단 동작을 검증.
- 제품: push가 유실돼도 알림함에 알림이 남고, 클릭 상태와 읽음 상태가 구분되는지 확인.

## 위험 요소와 완화

| 위험 | 완화 |
|---|---|
| Web Push는 표시·인지 보장 불가 | 상태명을 정확히 제한하고 보장 표현을 쓰지 않음 |
| JWT 없는 서비스 워커 요청 | receipt POST만 `permitAll()`로 열고 attempt별 고엔트로피 token hash 검증 |
| 재시도 전 payload의 지연 도착 | 재시도마다 새 attempt를 만들고 과거 attempt/token hash를 보존하되, Web Push TTL 기반 만료 전까지만 영수증 수락 |
| 영수증 API 장애가 사용자 알림을 방해 | 서비스 워커에서 timeout·`Promise.allSettled()`를 사용하고 표시/창 열기를 영수증 요청과 독립적으로 실행 |
| 오래된 token으로 상태 조작 또는 추적 데이터 장기 보존 | 256-bit token hash와 `receipt_expires_at`을 사용하고, 보존 기간은 승인된 개인정보 정책을 따른다 |
| payload 크기 초과 | 직렬화 byte 제한, 본문 절단, 알림함 상세 조회 |
| 다중 기기 상태 모호성 | 구독별 delivery와 attempt를 보존하고 목록에는 요약 상태만 노출 |
| 알림함 페이지 중복·누락 또는 상태 필터 성능 저하 | `(sentAt, id)` cursor와 `(user_id, read_at, sent_at, id)` 인덱스를 사용 |
| 프론트·백엔드 배포 순서 차이 | 구 payload 하위 호환 파서와 nullable 필드 사용 |

## 롤백 방법

1. 서비스 워커의 영수증 전송을 feature flag로 끈다. push 표시는 유지한다.
2. 백엔드는 receipt 수집만 중단하고 기존 `NotificationHistory` 발송 흐름은 유지한다.
3. 새 테이블/컬럼은 즉시 삭제하지 않고 보관 기간 후 별도 승인 migration으로 제거한다.

## 의사결정 로그

- 2026-07-17: Web Push provider 2xx는 최종 기기 도달이 아니라 provider 수락으로만 본다.
- 2026-07-17: 사용자 단위 `NotificationHistory`는 중복 방지·발송 요약으로 유지하고, 구독별 delivery와 시도별 attempt를 분리한다.
- 2026-07-17: JWT 없는 receipt POST는 `permitAll()`과 attempt별 opaque token hash 검증으로 보호한다.
- 2026-07-17: 재시도는 새 attempt를 만들며, 지연 수신을 위해 과거 token hash를 보존한다.
- 2026-07-17: `OPENED`와 읽음을 분리하고 사용자 읽음은 `read_at`으로 기록한다.
- 2026-07-24: 탈퇴 시 delivery/attempt 삭제는 `UserDataDeletionExecutor`의 raw SQL 순서에 맞추는 대신 FK `ON DELETE CASCADE`로 처리한다.
- 2026-07-24: delivery는 `(notification_history_id, subscription_id)` 기준 find-or-create로 만들어, 재시도 창에 새로 추가된 구독도 처리한다.
- 2026-07-24: receipt token은 attempt별 256-bit CSPRNG 값의 SHA-256 hash로 저장하고, Web Push TTL 기반 만료 전까지만 수락한다.
- 2026-07-24: 서비스 워커 영수증은 관측용이며, 영수증 API 오류가 알림 표시·앱 열기를 막지 않는 fail-open 동작을 보장한다.
- 2026-07-24: 알림함은 `sentAt DESC, id DESC` cursor pagination과 `readAt` 상태 필터를 사용한다.
- 2026-07-24: `WebPushSender`에 명시적 Web Push TTL을 지정하고, `receipt_expires_at`은 그 TTL 기준으로 계산한다. 기존 코드는 TTL을 넘기지 않아 기준값이 없었다.
- 2026-07-24: 알림함 cursor 조회는 JPQL이 지원하지 않는 row-value 비교 대신 OR 전개 또는 native query로 구현하며, 기존 `Pageable` 컨벤션과 의도적으로 다른 방식을 쓴다.
- 2026-07-24: custom metric 예산은 시계열 수가 아니라 메트릭 이름 개수(10개 이하) 기준이며, 이번 추가로 7개→8개가 된다.
- 2026-07-24 (PR1 구현 중): 탈퇴 삭제는 FK `ON DELETE CASCADE` 단독이 아니라 `UserDataDeletionExecutor.DELETE_STATEMENTS`에 `notification_delivery_attempts`/`notification_deliveries` 삭제문을 명시적으로 추가하는 방식과 **함께** 쓴다. `ddl-auto=create-drop`으로 생성되는 테스트 스키마는 이 코드베이스의 기존 `NotificationHistory`/`NotificationSubscription`처럼 순수 `@Column` FK라 Hibernate가 FK 제약을 생성하지 않으므로, cascade만으로는 테스트로 검증할 수 없다. 명시적 삭제문이 1차 보장 수단이고, migration의 `ON DELETE CASCADE`는 이 executor를 거치지 않는 경로에 대한 방어선이다. 기존 `notification_history`/`notification_subscriptions` → `users` 관계도 이미 이 이중 구조를 쓰고 있어 새 패턴이 아니다.
- 2026-07-24 (PR1 머지): 알림함 API(PR4)는 `notification_deliveries`/`notification_delivery_attempts`(PR1) 외 다른 진행 중 작업과 파일이 겹치지 않아 별도 브랜치로 분리하지 않고 PR1과 함께 `#106`으로 develop에 머지했다. PR2(발송 경로)는 PR1 커밋 시점에서 분기해 별도로 진행했으며, `NotificationDelivery`/`NotificationDeliveryAttempt`/`NotificationDeliveryRepository`에서 PR1 계열과 서로 다른 메서드를 추가한 add/add 충돌만 발생해 양쪽을 합치는 것으로 해결했다(도메인 메서드는 PR2가, `findAllByNotificationHistoryIdIn`은 PR1 계열이 추가).

## 완료 조건

- [ ] provider 수락, 수신, 표시, 클릭, 읽음이 서로 다른 의미로 기록된다.
- [ ] 한 사용자의 여러 Web Push 구독 및 재시도 attempt 결과를 개별 추적할 수 있다.
- [ ] JWT 없는 service worker 영수증이 token 검증을 통과할 때만 기록된다.
- [ ] 만료된 receipt token은 영수증을 기록할 수 없고, 영수증 API 장애가 알림 표시나 앱 열기를 막지 않는다.
- [ ] 사용자는 push 유실 여부와 관계없이 알림함에서 미확인 알림을 확인할 수 있다.
- [ ] 알림함의 상태 필터와 cursor pagination이 중복·누락 없이 동작하며 인덱스가 이를 지원한다.
- [ ] 기존 발송·중복 방지·재시도 테스트와 새 보안/통합 테스트가 통과한다.
- [ ] DB 스키마, API, 관측성, incident runbook이 구현과 일치한다.

## 작업 후 문서 업데이트

- [ ] `docs/db_schema.md`
- [ ] `docs/api-guide.md`
- [ ] `docs/engineering/observability.md`
- [ ] `docs/engineering/incident-runbook.md`
- [ ] 프론트 저장소의 서비스 워커/알림함 사용자 문서
