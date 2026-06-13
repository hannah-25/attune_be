# API Guide

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
      "goalCount": 3
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

## UserSetting 알림 필드 (변경 사항)

`PATCH /v1/settings` 요청/응답에 다음 두 필드가 추가됐다.

| 필드 | 기본값 | 설명 |
|------|--------|------|
| `communityNotification` | true | 커뮤니티 댓글 알림 수신 여부 |
| `todoNotification` | true | Todo 마감 알림 수신 여부 |
