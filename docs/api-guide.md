# API Guide

## Web Push 영수증

### POST /v1/notification-delivery-attempts/{deliveryAttemptId}/events

서비스 워커가 push 도달 단계(`RECEIVED`/`DISPLAYED`/`OPENED`)를 기록한다. 서비스 워커는 JWT를 갖고 있지 않으므로 이 경로만 인증 없이 허용하고, attempt id와 발급 시 함께 내려준 `receiptToken`으로만 요청을 검증한다.

**인증:** 불필요 (JWT 없음). `receiptToken`이 attempt별 검증 수단이다.

**Request**

```json
{
  "event": "RECEIVED",
  "receiptToken": "opaque-random-token"
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `event` | string | 필수 | `RECEIVED`, `DISPLAYED`, `OPENED` 중 하나. 그 외 값은 무시되고 204로 응답한다 |
| `receiptToken` | string | 필수 | push payload에 함께 내려준 opaque token. attempt id와 짝이 맞아야 한다 |

**Response 204**

정상 처리, 중복 요청, 존재하지 않는 attempt id, token 불일치, 만료된 token 모두 동일하게 `204 No Content`를 반환한다. attempt 존재 여부를 외부에 노출하지 않기 위한 의도적 설계다. 서버 내부에서는 이 다섯 가지 결과를 `attune.notification.delivery.receipts` 메트릭의 `outcome` 태그(`accepted`/`duplicate`/`invalid`/`expired`)로만 구분한다.

**Response 429**

attempt 단위, 클라이언트 IP 단위, 전역 단위 rate limit 중 하나라도 초과하면 `429 Too Many Requests`를 반환한다.

**동작 참고**

- 중복 요청은 최초 기록 시각을 바꾸지 않는다.
- `DISPLAYED`가 `RECEIVED`보다 먼저 도착하면(또는 `OPENED`가 `DISPLAYED`/`RECEIVED`보다 먼저 도착하면) 비어 있는 선행 단계를 같은 시각으로 backfill한다 — 이 backfill된 값은 실제 관측이 아닌 추정치다.
- token은 `receipt_expires_at`(발송 시 지정한 Web Push TTL 기준) 이전까지만 유효하다.

## Notification inbox

### GET /v1/notifications?status=&cursor=&size=

현재 사용자의 알림함을 `sentAt DESC, id DESC` 순서로 cursor pagination 한다. `status`는 생략하거나 `UNREAD`/`READ`로 필터할 수 있다. 알림은 push 수신 여부와 무관하게 `NotificationHistory` 단위로 반환한다.

**인증:** 필요 (JWT)

| Query parameter | 타입 | 필수 | 설명 |
|---|---|---|---|
| `status` | string | 선택 | `UNREAD`(`readAt`이 없음) 또는 `READ`(`readAt`이 있음). 생략하면 모두 조회 |
| `cursor` | string | 선택 | 직전 응답의 `nextCursor`. 클라이언트가 내용을 해석하지 않는 불투명 값 |
| `size` | number | 선택 | 페이지 크기. 기본 `20`, 범위 `1..100` |

**Response 200**

```json
{
  "notifications": [
    {
      "id": 42,
      "alarmType": "MEDICATION",
      "title": "복약 시간",
      "body": "복약 시간이 됐어요.",
      "url": "/medication",
      "status": "SENT",
      "deliveryStatus": "DISPLAYED",
      "sentAt": "2026-07-24T10:00:00",
      "readAt": null
    }
  ],
  "nextCursor": "MjAyNi0wNy0yNFQxMDowMDowMHw0Mg"
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `notifications` | array | 현재 페이지의 알림 목록 |
| `notifications[].url` | string | 이동할 내부 경로. 저장값이 없으면 `/home` |
| `notifications[].status` | string | 발송 이력 상태: `SENDING` / `SENT` / `FAILED` / `SKIPPED` |
| `notifications[].deliveryStatus` | string\|null | 구독별 delivery 중 가장 진전된 상태: `PROVIDER_ACCEPTED` / `RECEIVED` / `DISPLAYED` / `OPENED`. 실제 기기 표시나 인지를 보장하지 않음 |
| `notifications[].readAt` | LocalDateTime\|null | 사용자가 알림함에서 읽음 처리한 최초 시각. 클릭 상태(`OPENED`)와 별개 |
| `nextCursor` | string\|null | 다음 페이지 cursor. 다음 페이지가 없으면 `null` |

### PATCH /v1/notifications/{notificationHistoryId}/read

현재 사용자의 알림을 읽음 처리한다. 이미 읽은 알림은 최초 `readAt`을 유지한 채 성공한다.

**인증:** 필요 (JWT)

**Response 204**

다른 사용자의 알림이거나 존재하지 않는 ID는 `404`를 반환한다.

---

## Onboarding

### GET /v1/onboarding/history

자가 체크(ASRS) 이력 목록을 조회한다. 완료한 순서의 역순(최신 순)으로 반환한다.

**인증:** 필요 (JWT)

**Response 200**

```json
{
  "records": [
    {
      "id": "1",
      "doneAt": "2026-06-13T10:00:00",
      "inattentionScore": 12,
      "hyperactivityScore": 6,
      "goalCount": 3,
      "isQuickOnboarding": false
    }
  ]
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | string | 이력 고유 ID (상세 페이지 이동용) |
| `doneAt` | ISO 8601 string | ASRS 완료 시각 |
| `inattentionScore` | number | 부주의 점수 (Q1-9 합계) |
| `hyperactivityScore` | number | 과잉행동·충동성 점수 (Q10-18 합계) |
| `goalCount` | number | 현재 활성화된 치료 목표 수 |
| `isQuickOnboarding` | boolean | 빠른 온보딩 이력 여부 |

이력이 없으면 `records: []`를 반환한다.

---

### GET /v1/onboarding/history/{id}

특정 자가 체크 이력의 상세 정보를 조회한다.

**인증:** 필요 (JWT)

**Path Parameters**

| 파라미터 | 설명 |
|---------|------|
| `id` | 이력 고유 ID (`GET /history` 응답의 `id` 값) |

**Response 200**

```json
{
  "id": "1",
  "doneAt": "2026-06-13T10:00:00",
  "inattentionScore": 12,
  "hyperactivityScore": 6,
  "symptom": {
    "description": "집중하기 어렵고 계획을 세워도 실행이 잘 안 됩니다.",
    "emotionalEvent": "최근 직장에서 중요한 마감을 놓쳤습니다.",
    "isQuickOnboarding": false,
    "selectedSymptomTypes": null,
    "selectedFunctionalAreas": null
  },
  "goals": [
    { "id": 1, "title": "알람 울리면 바로 시작하기", "type": "TIME_MANAGEMENT" }
  ]
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `symptom.description` | string\|null | 증상 서술 (경로 A) |
| `symptom.emotionalEvent` | string\|null | 감정적 사건 서술 (경로 A) |
| `symptom.isQuickOnboarding` | boolean | 빠른 온보딩(경로 B) 여부 |
| `symptom.selectedSymptomTypes` | string[]\|null | 취약 증상 영역 (경로 B) |
| `symptom.selectedFunctionalAreas` | string[]\|null | 취약 기능 영역 (경로 B) |
| `goals` | array | 현재 활성화된 치료 목표 목록 |

**Response 404** — 해당 ID의 이력이 없거나 다른 사용자의 이력인 경우

---

## Consultation

### GET /v1/consultations

진료 목록을 조회한다.

**인증:** 필요 (JWT)

**Response 200**

```json
{
  "consultations": [
    {
      "consultationId": 1,
      "consultationDate": "2026-06-01T10:00:00",
      "place": "스마일 정신건강 클리닉",
      "doctorName": "김지연",
      "prescriptionNote": null
    }
  ]
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `consultationId` | number | 진료 고유 ID (수정/삭제 API 호출용) |
| `consultationDate` | ISO 8601 string | 진료 일시 |
| `place` | string | 병원명 |
| `doctorName` | string\|null | 담당 의사 이름 (편집 화면 pre-fill용) |
| `prescriptionNote` | string\|null | 처방 메모 |

### GET /v1/consultations/{consultationId}/questions

상담 전 질문 목록을 조회한다.

**인증:** 필요 (JWT)

**Response 200**

```json
[
  { "questionId": 1, "text": "약 용량 조정 가능한가요?" },
  { "questionId": 2, "text": "수면제와 함께 복용해도 괜찮을까요?" }
]
```

---

### POST /v1/consultations/{consultationId}/questions

상담 전 질문을 추가한다.

**인증:** 필요 (JWT)

**Request Body**

| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `text` | 필수 | 질문 내용 |

**Response 201**

```json
{ "questionId": 3, "text": "아침 식욕이 없어요. 다른 약으로 바꿔야 할까요?" }
```

---

### DELETE /v1/consultations/{consultationId}/questions/{questionId}

상담 전 질문을 개별 삭제한다.

**인증:** 필요 (JWT)

**Response 204** (No Content)

**Response 404** — 해당 상담 일정 또는 질문이 없는 경우

---

## Todo

### GET /v1/todos?date=YYYY-MM-DD 또는 ?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD

- `date`: 기존 단일 날짜 조회입니다.
- `startDate`, `endDate`: 양 끝 날짜를 포함한 범위 조회입니다. 두 값은 함께 전달해야 하며 `startDate`는 `endDate`보다 늦을 수 없습니다.
- 응답은 두 방식 모두 `todos` 배열과 동일한 TodoItem 형식을 반환합니다.

## Medication

### GET /v1/medications

약 이름 또는 성분명으로 마스터 약물을 검색한다. `q` 생략 시 전체 목록을 반환한다.

**인증:** 필요 (JWT)

**Query Parameters**

| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `q` | 선택 | 검색 키워드 (약 이름 또는 성분명, 대소문자 무시) |

**Response 200**

```json
[
  {
    "medicationId": 1,
    "name": "콘서타",
    "ingredient": "메틸페니데이트",
    "dosageOptions": [
      { "dosageId": 3, "amount": 18.00 },
      { "dosageId": 4, "amount": 27.00 },
      { "dosageId": 5, "amount": 36.00 }
    ]
  },
  {
    "medicationId": 2,
    "name": "스트라테라",
    "ingredient": "아토목세틴",
    "dosageOptions": [
      { "dosageId": 6, "amount": 10.00 },
      { "dosageId": 7, "amount": 18.00 },
      { "dosageId": 8, "amount": 25.00 },
      { "dosageId": 9, "amount": 40.00 }
    ]
  }
]
```

`dosageOptions`는 활성화된 용량(`isActive=true`)만 포함하며, amount 오름차순 정렬된다.  
검색 결과가 없으면 빈 배열 `[]`을 반환한다.

---

## Journal Tags (태그 관리)

### GET /v1/journals/tags?category=&manage=false

사용자의 태그 목록을 조회한다. SYSTEM 태그와 USER 태그를 함께 반환한다.

**인증:** 필요 (JWT)

**Query Parameters**

| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `category` | 필수 | `CONDITION` / `SIDE_EFFECT` / `TROUBLE` |
| `manage` | 선택 (기본 false) | false: enabled && visible 태그만. true: 비활성 태그 포함 전체 |

**Response 200**

```json
[
  { "tagId": 1, "category": "CONDITION", "name": "집중 어려움", "tagType": "INATTENTION", "scope": "SYSTEM", "enabled": true, "visible": true }
]
```

---

### POST /v1/journals/tags

사용자 태그를 생성한다. 동일 name+tagType의 비활성 태그가 있으면 재활성화(200), 새로 생성하면 201.

**인증:** 필요 (JWT)

**Request Body**

```json
{ "category": "CONDITION", "name": "두통", "tagType": "OTHER", "visible": true }
```

**Response 201** (신규) / **200** (재활성화)

```json
{ "tagId": 10, "category": "CONDITION", "name": "두통", "tagType": "OTHER", "scope": "USER", "enabled": true, "visible": true }
```

---

### PATCH /v1/journals/tags/{tagId}/preference

태그의 enabled/visible 상태를 변경한다. `enabled=false`이면 `visible`은 자동으로 false 처리.

**인증:** 필요 (JWT)

**Request Body**

```json
{ "enabled": true, "visible": false }
```

**Response 200** — 갱신된 태그 응답 (GET 목록과 동일 형식)

---

### DELETE /v1/journals/tags/{tagId}

태그를 삭제한다. SYSTEM 태그는 preference만 비활성화, USER 태그는 엔티티도 비활성화.

**인증:** 필요 (JWT)

**Response 204**

---

### POST /v1/journals/tags/{tagId}/checks

특정 날짜에 태그를 체크인한다. 동일 날짜에 이미 체크인되어 있으면 기존 응답 반환(멱등).

**인증:** 필요 (JWT)

**Request Body**

```json
{ "journalDate": "2026-06-23" }
```

`journalDate`는 오늘 이전(사용자 최신 timezone 기준)이어야 한다. 미래 날짜는 400.

**Response 200**

```json
{ "tagId": 1, "category": "CONDITION", "name": "집중 어려움", "tagType": "INATTENTION", "journalDate": "2026-06-23", "checkedAt": "2026-06-23T09:30:00" }
```

---

### DELETE /v1/journals/tags/{tagId}/checks?date=

특정 날짜의 체크인을 취소한다. 체크인이 없어도 204 반환(멱등).

**인증:** 필요 (JWT)

**Query Parameters**

| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `date` | 필수 | 취소할 날짜 (YYYY-MM-DD) |

**Response 204**

---

## Journal (일지)

### GET /v1/journals?startDate=&endDate=

기간 내 **모든 날짜**의 활성 태그와 체크 내역을 한 번에 반환한다. 홈 화면 초기 로드 시 개별 날짜 조회 7회 대신 이 API 1회로 대체한다.

**인증:** 필요 (JWT)

**Query Parameters**

| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `startDate` | 필수 | 시작 날짜 (YYYY-MM-DD) |
| `endDate` | 필수 | 종료 날짜 (YYYY-MM-DD) |

최대 조회 기간은 **31일**이다. 초과 시 400 응답.

**Response 200**

```json
{
  "activeTags": {
    "conditions": [{ "tagId": 1, "condition": "집중 어려움", "conditionType": "INATTENTION", "visible": true }],
    "sideEffects": [],
    "troubles": [],
    "goals": [{ "goalId": 3, "content": "알람 울리면 바로 시작하기" }]
  },
  "journals": [
    {
      "date": "2026-06-21",
      "checked": {
        "conditions": [{ "tagId": 1, "condition": "집중 어려움", "conditionType": "INATTENTION", "checkedAt": "2026-06-21T09:00:00" }],
        "sideEffects": [],
        "troubles": [],
        "sleep": { "sleepHour": 7.5, "sleepQuality": "GOOD" },
        "meal": { "ateBreakfast": true, "ateLunch": true, "ateDinner": false },
        "goals": [{ "goalId": 3, "content": "알람 울리면 바로 시작하기", "score": 1 }],
        "memo": "오늘은 집중이 잘 됐다."
      }
    }
  ]
}
```

날짜 범위 내 데이터가 없는 날도 `checked`가 모두 빈 값으로 포함된다.

---

### GET /v1/journals/dates?startDate=&endDate=

기간 내 일지 데이터가 **존재하는 날짜 목록**만 반환한다. (구 `GET /v1/journals`)

**인증:** 필요 (JWT)

**Response 200**

```json
{ "dates": ["2026-06-19", "2026-06-21"] }
```

---

### GET /v1/journals/{date}

단일 날짜의 일지 상세를 조회한다.

**인증:** 필요 (JWT)

---

## Notice (공지사항)

### GET /v1/notices/{noticeId}

공지 상세를 조회한다. `isPinned` 필드를 포함하므로 별도 목록 조회 불필요.

**인증:** 필요 (JWT)

**Response 200**

```json
{
  "noticeId": 1,
  "title": "5월 업데이트 안내",
  "content": "새로운 기능이 추가되었습니다.",
  "isPinned": true,
  "createdAt": "2026-06-01T10:00:00",
  "updatedAt": "2026-06-01T10:00:00"
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `noticeId` | number | 공지 고유 ID |
| `title` | string | 공지 제목 |
| `content` | string | 공지 내용 |
| `isPinned` | boolean | 상단 고정 여부 |
| `createdAt` | LocalDateTime | 등록 일시 |
| `updatedAt` | LocalDateTime | 수정 일시 |

**Response 404** — 해당 ID의 공지가 없거나 삭제된 경우

---

## Alarm (알람 구독)

### POST /v1/alarm/subscriptions

푸시 알람 수신을 위한 구독 정보를 등록하거나 갱신한다. `endpoint`가 있으면 Web Push, 없으면 FCM/APNs 토큰 기반으로 처리된다.

**인증:** 필요 (JWT)

**Request Body**

| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `platform` | 필수 | WEB / ANDROID / IOS |
| `provider` | 필수 | WEB_PUSH / FCM / APNS |
| `endpoint` | Web Push 필수 | Web Push API endpoint URL |
| `p256dh` | Web Push 필수 | Web Push 공개키 |
| `auth` | Web Push 필수 | Web Push 인증 시크릿 |
| `token` | FCM/APNs 필수 | FCM 또는 APNs 등록 토큰 |

**Response 201**

```json
{
  "id": 1,
  "platform": "WEB",
  "provider": "WEB_PUSH",
  "enabled": true
}
```

---

### GET /v1/alarm/subscriptions

현재 사용자의 구독 활성화 여부를 조회한다.

**인증:** 필요 (JWT)

**Query Parameters**

| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `endpointOrToken` | 필수 | 조회할 endpoint URL 또는 FCM/APNs 토큰 |

**Response 200**

```json
{
  "enabled": true
}
```

인증된 사용자 본인의 구독만 조회되며, 구독이 없거나 비활성화된 경우에도 200과 `enabled: false`를 반환한다.

---

### DELETE /v1/alarm/subscriptions

구독을 비활성화한다.

**인증:** 필요 (JWT)

**Query Parameters**

| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `endpointOrToken` | 필수 | 비활성화할 endpoint URL 또는 FCM/APNs 토큰 |

**Response 204** (No Content)

---

## Admin Alarm (관리자 마케팅 알람)

### POST /v1/admin/notices/{noticeId}/push

`marketingNotification=true`인 전체 사용자에게 공지 내용을 비동기 푸시 발송한다.

**인증:** 필요 (JWT, ADMIN 권한)

**Response 202** (Accepted) — 발송 작업이 백그라운드에서 시작됨

---

## UserSetting 필드 (변경 사항)

`GET /v1/users/settings`, `PATCH /v1/users/settings` 응답에 `timezone`이 포함된다.
`PATCH /v1/users/settings` 요청에는 변경할 필드만 전달할 수 있다.

| 필드 | 기본값 | 설명 |
|------|--------|------|
| `communityNotification` | true | 커뮤니티 댓글 알림 수신 여부 |
| `todoNotification` | true | Todo 마감 알림 수신 여부 |
| `timezone` | `Asia/Seoul` | 사용자 최신 IANA timezone ID. 예: `Asia/Seoul`, `America/New_York` |

`timezone` 검증 규칙:

- 요청에 없거나 `null`이면 기존 값을 유지한다.
- 빈 문자열이거나 유효하지 않은 IANA timezone이면 400.

요청 예:

```json
{ "timezone": "America/New_York" }
```

---

## 약물 치료 경과 리포트 API

### AI 분석 동의 관리

| 메서드 | 경로 | 설명 | 인증 |
|--------|------|------|------|
| `PUT` | `/v1/ai-analysis-consent` | AI 분석 동의 | 필요 |
| `DELETE` | `/v1/ai-analysis-consent` | AI 분석 동의 철회 | 필요 |

**응답**: 204 No Content

---

### 리포트 API

| 메서드 | 경로 | 설명 | 인증 |
|--------|------|------|------|
| `GET` | `/v1/medication-analysis/availability` | 리포트 생성 가능 여부 확인 | 필요 |
| `GET` | `/v1/medication-analysis/summary` | 복용 통계 요약 (리포트 생성 없음) | 필요 |
| `POST` | `/v1/medication-analysis/reports` | 리포트 생성 | 필요 |
| `GET` | `/v1/medication-analysis/reports` | 리포트 목록 조회 | 필요 |
| `GET` | `/v1/medication-analysis/reports/{reportId}` | 리포트 단건 조회 | 필요 |

#### GET /v1/medication-analysis/availability

Query Params: `startDate` (ISO date), `endDate` (ISO date)

```json
{
  "available": true,
  "recordedDays": 24,
  "unavailableReasons": []
}
```

#### POST /v1/medication-analysis/reports

```json
{
  "periodStart": "2026-05-15",
  "periodEnd": "2026-06-13",
  "includeMemoExcerpts": true
}
```

응답 `ReportDetailResponse`:

| 필드 | 설명 |
|------|------|
| `reportId` | 리포트 ID |
| `periodStart` / `periodEnd` | 분석 기간 |
| `status` | PENDING / COMPLETED / FAILED / OUTDATED |
| `outdated` | 원본 데이터 변경 여부 (조회 시점 판단) |
| `snapshotJson` | 서버 분석 스냅샷 JSON |
| `aiResultJson` | Gemini AI 분석 결과 (미동의·실패 시 null) |
| `generatedAt` | 생성 시각 |

---

## Admin

> **인증:** 모든 Admin API는 `ROLE_ADMIN` 권한을 가진 JWT 토큰이 필요합니다.

---

### POST /v1/admin/members/{memberUuid}/status

회원 상태를 변경한다. 허용된 전환 이외의 요청은 409를 반환한다.

**허용 전환표**

| 현재 상태 | 변경 가능 상태 |
|-----------|---------------|
| `PENDING` | `ACTIVE` |
| `ACTIVE` | `SUSPENDED` |
| `SUSPENDED` | `ACTIVE` |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `memberUuid` | UUID string | 대상 회원 ID |

**Request Body**

```json
{
  "status": "ACTIVE",
  "reason": "본인 확인 후 수동 승인"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `status` | string | 필수 | 변경할 상태 (`ACTIVE` / `SUSPENDED`) |
| `reason` | string | 필수 | 처리 사유 (공백 제외 5자 이상, 감사 로그에 기록됨) |

**Response 200** — 변경된 회원 정보 (`AdminMemberResponse` 구조, 회원 목록 응답의 `members` 항목과 동일)

**Error Cases**

| 상태 코드 | 설명 |
|-----------|------|
| `400` | `reason` 누락 또는 5자 미만 |
| `404` | 해당 회원 없음 |
| `409` | 허용되지 않는 상태 전환 (e.g. `WITHDRAWAL → SUSPENDED`) |

---

### GET /v1/admin/members

회원 목록을 조회한다. 검색어·상태 필터와 페이지네이션을 지원한다.

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| `query` | string | 선택 | - | 이메일 또는 닉네임 부분 검색 |
| `status` | string | 선택 | - | 회원 상태 필터 (`PENDING` / `ACTIVE` / `SUSPENDED` / `WITHDRAWAL`) |
| `page` | number | 선택 | `0` | 페이지 번호 (0-based) |
| `size` | number | 선택 | `20` | 페이지 크기 (1~100) |

**Response 200**

```json
{
  "members": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "email": "user@example.com",
      "nickname": "홍길동",
      "status": "WITHDRAWAL",
      "provider": "KAKAO",
      "createdAt": "2026-01-01T00:00:00Z",
      "lastLoginAt": "2026-06-01T12:00:00Z",
      "withdrawalRequestedAt": "2026-06-15T09:00:00Z",
      "withdrawalScheduledAt": "2026-06-22T09:00:00Z"
    }
  ],
  "summary": {
    "total": 120,
    "pending": 5,
    "active": 100,
    "suspended": 3,
    "withdrawal": 12
  },
  "page": 0,
  "size": 20,
  "totalElements": 120,
  "totalPages": 6
}
```

**members 필드**

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | UUID string | 회원 고유 ID |
| `email` | string | 이메일 |
| `nickname` | string | 닉네임 |
| `status` | string | `PENDING` / `ACTIVE` / `SUSPENDED` / `WITHDRAWAL` |
| `provider` | string | `GOOGLE` / `KAKAO` / `APPLE` |
| `createdAt` | ISO 8601 (UTC) | 가입 일시 |
| `lastLoginAt` | ISO 8601 (UTC) | 마지막 로그인 일시 (없으면 `null`) |
| `withdrawalRequestedAt` | ISO 8601 (UTC) | 탈퇴 요청 일시 (`WITHDRAWAL` 상태일 때만 존재) |
| `withdrawalScheduledAt` | ISO 8601 (UTC) | 탈퇴 예정 일시 (`withdrawalRequestedAt` + 유예 기간) |

**summary 필드**

| 필드 | 타입 | 설명 |
|------|------|------|
| `total` | number | 전체 회원 수 |
| `pending` | number | 이메일 인증 대기 중 |
| `active` | number | 정상 활성 |
| `suspended` | number | 정지된 회원 |
| `withdrawal` | number | 탈퇴 진행 중 (유예기간) |

---

### POST /v1/admin/members/{memberId}/withdrawal/cancel

탈퇴 요청 중인 회원의 탈퇴를 취소한다. 회원 상태가 `WITHDRAWAL`인 경우에만 가능하다.

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `memberId` | UUID string | 대상 회원 ID |

**Request Body**

```json
{
  "reason": "본인 요청으로 탈퇴 철회"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `reason` | string | 필수 | 처리 사유 (공백 제외 5자 이상) |

**Response 200** — 취소 후 업데이트된 회원 정보 (`AdminMemberResponse` 구조, 위 members 필드와 동일)

**Error Cases**

| 상태 코드 | 설명 |
|-----------|------|
| `400` | `reason` 누락 또는 5자 미만 |
| `404` | 해당 회원 없음 |
| `409` | `WITHDRAWAL` 상태가 아닌 회원에게 요청 |

---

### POST /v1/admin/members/{memberId}/withdrawal/complete

탈퇴 요청 중인 회원을 즉시 완전 삭제한다. **복구 불가능한 작업이므로 주의.**

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `memberId` | UUID string | 삭제할 회원 ID |

**Request Body**

```json
{
  "reason": "유예기간 만료 전 본인 재요청"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `reason` | string | 필수 | 처리 사유 (공백 제외 5자 이상) |

**Response 204** — 본문 없음

**Error Cases**

| 상태 코드 | 설명 |
|-----------|------|
| `400` | `reason` 누락 또는 5자 미만 |
| `404` | 해당 회원 없음 |
| `409` | `WITHDRAWAL` 상태가 아닌 회원에게 요청 |

---

### POST /v1/admin/members/{memberId}/withdrawal/soft-delete

탈퇴 요청 중인 회원을 소프트 삭제한다. 데이터는 유지하고 상태만 `DELETED`로 변경하며, 해당 계정으로 더 이상 로그인할 수 없다.

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `memberId` | UUID string | 소프트 삭제할 회원 ID |

**Request Body**

```json
{
  "reason": "개인정보 보관 필요에 의한 소프트 삭제"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `reason` | string | 필수 | 처리 사유 (공백 제외 5자 이상) |

**Response 204** — 본문 없음

**Error Cases**

| 상태 코드 | 설명 |
|-----------|------|
| `400` | `reason` 누락 또는 5자 미만 |
| `404` | 해당 회원 없음 |
| `409` | `WITHDRAWAL` 상태가 아닌 회원에게 요청, 또는 관리자 본인 계정 |

---

### GET /v1/admin/audit-logs

관리자 작업 감사 로그를 최신순으로 조회한다.

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| `limit` | number | 선택 | `10` | 조회 건수 (1~100) |

**Response 200**

```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440001",
    "action": "WITHDRAWAL_CANCELLED",
    "targetReference": "a1b2c3d4...",
    "targetLabel": "user@example.com",
    "administrator": "admin@attune.com",
    "reason": "본인 요청으로 탈퇴 철회",
    "createdAt": "2026-06-20T10:30:00Z"
  }
]
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | UUID string | 로그 고유 ID |
| `action` | string | `WITHDRAWAL_CANCELLED` (탈퇴 취소) / `MEMBER_DELETED` (회원 삭제) |
| `targetReference` | string | 대상 회원 ID의 해시값 (개인정보 보호용) |
| `targetLabel` | string | 대상 회원 이메일 |
| `administrator` | string | 처리한 관리자 이메일 |
| `reason` | string | 처리 사유 |
| `createdAt` | ISO 8601 (UTC) | 작업 일시 |

---

### POST /v1/admin/notices

공지사항을 등록한다.

**Request Body**

```json
{
  "title": "5월 업데이트 안내",
  "content": "새로운 기능이 추가되었습니다.",
  "isPinned": true,
  "sendNotification": true,
  "sendEmail": false
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `title` | string | 필수 | 공지 제목 |
| `content` | string | 필수 | 공지 내용 |
| `isPinned` | boolean | 필수 | 상단 고정 여부 |
| `sendNotification` | boolean | 필수 | 등록 시 전체 회원(마케팅 수신 동의)에게 푸시 발송 여부 |
| `sendEmail` | boolean | 필수 | 등록 시 전체 ACTIVE 회원에게 이메일 발송 여부 |

> `sendNotification`/`sendEmail` 발송은 비동기로 처리되며, 응답 시점에는 발송이 완료되지 않을 수 있습니다.

**Response 201**

```json
{
  "noticeId": 42,
  "title": "5월 업데이트 안내",
  "createdAt": "2026-06-20T10:00:00"
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `noticeId` | number | 등록된 공지 ID |
| `title` | string | 공지 제목 |
| `createdAt` | LocalDateTime (UTC) | 등록 일시 |

---

### GET /v1/admin/terms

전체 약관 목록을 최신 등록순으로 조회한다.

**Response 200**

```json
[
  {
    "id": 5,
    "type": "MARKETING_CONSENT",
    "version": 2,
    "effectiveAt": "2026-06-01T00:00:00",
    "createdAt": "2026-05-20T09:00:00"
  }
]
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | number | 약관 고유 ID |
| `type` | string | `TERMS_OF_SERVICE` / `PRIVACY_POLICY` / `MARKETING_CONSENT` / `AI_ANALYSIS_CONSENT` |
| `version` | number | 약관 버전 |
| `effectiveAt` | LocalDateTime (UTC) | 약관 시행일 |
| `createdAt` | LocalDateTime (UTC) | 등록 일시 |

---

### POST /v1/admin/marketing/push

마케팅 수신 동의(앱 설정 ON) + ACTIVE 상태 회원 전체에게 푸시를 발송한다. 발송은 비동기로 처리되며 응답은 즉시 반환된다.

**Request Body**

```json
{
  "title": "이번 주 특별 리포트를 확인해보세요",
  "body": "지난 4주간의 복약 패턴을 분석했어요.",
  "targetUrl": "/medication"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `title` | string | 필수 | 푸시 제목 |
| `body` | string | 필수 | 푸시 본문 |
| `targetUrl` | string | 선택 | 클릭 시 이동 경로 (null이면 클라이언트 기본 동작) |

**Response 200**

```json
{
  "sentAt": "2026-06-20T10:00:00Z",
  "targetCount": 83
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `sentAt` | ISO 8601 (UTC) | 발송 시작 시각 |
| `targetCount` | number | 발송 대상 회원 수 |

**Error Cases**

| 상태 코드 | 설명 |
|-----------|------|
| `400` | `title` 또는 `body` 누락 |
| `409` | 발송 가능 대상 없음 (`message`: "발송 가능한 마케팅 수신 동의 회원이 없습니다.") |
