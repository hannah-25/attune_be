# User Medications API Spec

## 1. 사용자 복약 목록 조회

- Method: `GET`
- Path: `/v1/user-medications`
- Auth: `Authorization: Bearer <token>` 필요

### Request

- Query Parameter: 없음
- Body: 없음

### Response

- Status: `200 OK`
- Body Type: `UserMedicationListItemResponse[]`

```json
[
  {
    "userMedicationId": 101,
    "medicationId": 12,
    "medicationName": "Concerta",
    "medicationDosageId": 33,
    "dosageAmount": 18.00,
    "consultationId": 77,
    "isActive": true,
    "startedAt": "2026-05-30",
    "endAt": null,
    "schedules": [
      {
        "scheduleId": 1001,
        "doseTime": "08:30:00",
        "label": "아침"
      },
      {
        "scheduleId": 1002,
        "doseTime": "13:00:00",
        "label": "점심"
      }
    ]
  }
]
```

### Response Field Spec

| Field | Type | Nullable | Description |
|---|---|---|---|
| userMedicationId | number(int64) | N | 사용자 복약 ID (`user_medications.id`) |
| medicationId | number(int64) | N | 기준 약 ID (`medications.id`) |
| medicationName | string | N | 기준 약 이름 |
| medicationDosageId | number(int64) | N | 선택된 용량 옵션 ID (`medication_dosages.id`) |
| dosageAmount | number(decimal) | N | 선택된 용량 값 |
| consultationId | number(int64) | Y | 연결된 진료 ID (`consultations.id`) |
| isActive | boolean | N | 복약 활성 여부 |
| startedAt | string(date) | Y | 복약 시작일 (`yyyy-MM-dd`) |
| endAt | string(date) | Y | 복약 종료일 (`yyyy-MM-dd`) |
| schedules | array | N | 복용 스케줄 목록 |
| schedules[].scheduleId | number(int64) | N | 스케줄 ID (`user_medication_schedules.id`) |
| schedules[].doseTime | string(time) | N | 복용 시각 (`HH:mm:ss`) |
| schedules[].label | string | Y | 스케줄 라벨 |

### 정렬 규칙

- 복약 목록: `isActive DESC`, `createdAt DESC`, `id DESC`
- 각 복약의 `schedules`: `doseTime ASC`

### Error

| Status | Condition |
|---|---|
| 401 | 인증 토큰 없음/만료/유효하지 않음 |
| 500 | 서버 내부 오류 |

## 2. 사용자 복약 수정

- Method: `PATCH`
- Path: `/v1/user-medications/{userMedicationId}`
- Auth: `Authorization: Bearer <token>` 필요

### Request

부분 수정(PATCH). 전달한 필드만 반영된다.

```jsonc
PATCH /v1/user-medications/1
{
  "endAt": "2026-07-31",          // 생략: 변경 안 함 / null: 종료일 제거
  "isActive": true,
  "alarmActive": false,
  "schedules": [                  // 생략: 복용 시간 일정 변경 안 함
    { "doseTime": "09:30", "label": "복용" }
  ]
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| endAt | string(date) \| null | N | 생략 시 변경 없음, `null`이면 종료일 제거 |
| isActive | boolean | N | 복약 활성 여부 |
| alarmActive | boolean | N | 알림 활성 여부 |
| schedules | array | N | 복용 시간 일정. **전달 시 전체 교체(full replace)**. POST의 `schedules`와 동일 형태(`doseTime`, `label`) |
| schedules[].doseTime | string(time) | Y(전달 시) | 복용 시각 (`HH:mm`) |
| schedules[].label | string | N | 스케줄 라벨 |

### schedules 전체 교체 동작

`schedules`를 전달하면 해당 복약의 복용 시간 일정을 전달된 목록으로 **전체 교체**한다. dose_time 기준으로 비교한다.

| 케이스 | 처리 |
|---|---|
| 요청 dose_time = 기존 활성 일정 | 유지 (라벨만 갱신) |
| 요청 dose_time = 기존 **비활성** 일정 | 재활성화 + 라벨 갱신 (행 재사용 → unique 충돌 회피) |
| 요청 dose_time = 신규 | 새 일정 생성 |
| 기존 활성 일정 ∉ 요청 | **비활성화(`is_active=false`)** — 물리 삭제하지 않음 |

- **복용 로그는 항상 보존된다.** 빠진 일정은 삭제하지 않고 비활성화하므로, 그 일정에 달린 과거 복용 로그(`user_medication_logs`)가 유지된다.
- 비활성 일정은 목록 조회·복약 알림에서 제외되고, 로그 조회에는 그대로 노출된다.

### Response

- Status: `200 OK`
- Body Type: `UpdateMedicationResponse`

```json
{
  "userMedicationId": 1,
  "isActive": true,
  "updatedAt": "2026-06-26T10:00:00"
}
```

### Error

| Status | Condition |
|---|---|
| 400 | `schedules`가 빈 배열 / `doseTime` 누락 / 동일 `doseTime` 중복 / 활성 복약의 종료일이 과거 |
| 401 | 인증 토큰 없음/만료/유효하지 않음 |
| 404 | 사용자 복약 없음 |

## 3. 연관된 기존 엔드포인트

- `GET /v1/medications/standards/{medicationId}`: 기준 약 상세 조회
- `POST /v1/user-medications`: 사용자 복약 생성
- `GET /v1/user-medications/{userMedicationId}/logs`: 특정 복약 로그 조회
- `GET /v1/user-medications/logs`: 기간 복약 로그 조회
- `POST /v1/user-medications/{userMedicationId}/log/quick`: 빠른 복약 기록

### 기간 복약 로그 응답 메모

`GET /v1/user-medications/logs`의 각 로그 항목은 복약 체크 매칭을 위해 `scheduleId`를 포함한다.
`intakeTime`은 프론트가 `new Date(intakeTime)`으로 안전하게 파싱할 수 있도록 `+09:00` 오프셋을 포함한다.

```json
{
  "logs": [
    {
      "userMedicationId": 1,
      "scheduleId": 10,
      "name": "Concerta",
      "intakeTime": "2026-07-01T14:00:00+09:00",
      "taken": true
    }
  ]
}
```
