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

## 2. 연관된 기존 엔드포인트

- `GET /v1/medications/standards/{medicationId}`: 기준 약 상세 조회
- `POST /v1/user-medications`: 사용자 복약 생성
- `PATCH /v1/user-medications/{userMedicationId}`: 사용자 복약 수정
- `GET /v1/user-medications/{userMedicationId}/logs`: 특정 복약 로그 조회
- `GET /v1/user-medications/logs`: 기간 복약 로그 조회
- `POST /v1/user-medications/{userMedicationId}/log/quick`: 빠른 복약 기록
