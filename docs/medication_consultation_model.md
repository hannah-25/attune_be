# Medication-Consultation 데이터 모델 (현재 반영)

이 문서는 `medication`, `medication_dosage`, `user_medication`, `user_medication_schedule`, `consultation` 중심 설계를 현재 코드 기준으로 정리한 스펙입니다.  
`to-be`/`as-is`를 분리하지 않고, 각 항목의 변경 상태를 `new`, `deprecated`, `-`로 표시합니다.

## 1) 엔티티 관계

- `medications` 1:N `medication_dosages`
- `consultations` 1:N `user_medications` (`consultation_id`는 nullable)
- `users` 1:N `user_medications`
- `medication_dosages` 1:N `user_medications`
- `user_medications` 1:N `user_medication_schedules`
- `user_medication_schedules` 1:N `user_medication_logs`

## 2) 테이블 스펙

### 2.1 `medications`

| Column | Type | Null | Constraint | Change | 설명 |
|---|---|---|---|---|---|
| id | BIGINT | N | PK, AUTO_INCREMENT | - | 약 ID |
| name | VARCHAR(255) | N | UNIQUE (`uk_medications_name`) | - | 약 이름 |
| generic_name | VARCHAR(255) | Y |  | - | 성분명 |
| effect | TEXT | Y |  | - | 효능 |
| side_effect | TEXT | Y |  | - | 부작용 |
| description | TEXT | Y |  | - | 설명 |
| graph_url | TEXT | Y |  | - | 그래프 URL |
| image_url | TEXT | Y |  | - | 이미지 URL |
| formulation | VARCHAR(255) | Y |  | - | 제형 |
| typical_dosage_range | TEXT | Y |  | - | 일반 용량 범위 |
| drug_class | VARCHAR(255) | Y |  | - | 약물 계열 |
| source_url | TEXT | Y |  | - | 출처 URL |

### 2.2 `medication_dosages`

| Column | Type | Null | Constraint | Change | 설명 |
|---|---|---|---|---|---|
| id | BIGINT | N | PK, AUTO_INCREMENT | new | 용량 옵션 ID |
| medication_id | BIGINT | N | FK -> `medications.id` | new | 대상 약 |
| amount | DECIMAL(6,2) | N |  | new | 용량 값 |
| is_active | BOOLEAN | N | DEFAULT TRUE | new | 사용 가능 여부 |
| (medication_id, amount) | - | - | UNIQUE | new | 동일 약 용량 중복 방지 |
| unit | VARCHAR | - | - | deprecated | 제거됨 (불필요) |
| form | VARCHAR | - | - | deprecated | 제거됨 (`medications.formulation`로 관리) |

### 2.3 `consultations`

| Column | Type | Null | Constraint | Change | 설명 |
|---|---|---|---|---|---|
| id | BIGINT | N | PK, AUTO_INCREMENT | - | 진료 ID |
| user_id | UUID/BINARY(16) | N | FK -> `users.id` | - | 사용자 |
| consultation_date | DATETIME/TIMESTAMP | N |  | - | 진료 일시 |
| place | VARCHAR(255) | N |  | - | 장소 |
| doctor_name | VARCHAR(255) | Y |  | - | 의사명 |
| is_first_visit | BOOLEAN | N |  | - | 초진 여부 |
| summary_report | TEXT | Y |  | - | 요약 리포트 |
| pre_consultation_note | TEXT | Y |  | - | 진료 전 메모 |
| doctor_advice | TEXT | Y |  | - | 의사 조언 |
| prescription_note | TEXT | Y |  | - | 처방 메모 |
| next_treatment_goal | TEXT | Y |  | - | 다음 치료 목표 |
| alarm_settings | BOOLEAN | N | DEFAULT TRUE | - | 알림 설정 |
| is_deleted | BOOLEAN | N |  | - | 소프트 삭제 |
| created_at | DATETIME/TIMESTAMP | Y |  | - | 생성일 |
| updated_at | DATETIME/TIMESTAMP | Y |  | - | 수정일 |

### 2.4 `user_medications`

| Column | Type | Null | Constraint | Change | 설명 |
|---|---|---|---|---|---|
| id | BIGINT | N | PK, AUTO_INCREMENT | - | 사용자 복약 ID |
| user_id | UUID/BINARY(16) | N | FK -> `users.id` | - | 사용자 |
| consultation_id | BIGINT | Y | FK -> `consultations.id` | new | 연계 진료 (nullable 허용) |
| medication_dosage_id | BIGINT | N | FK -> `medication_dosages.id` | new | 선택한 용량 옵션 |
| is_active | BOOLEAN | N | DEFAULT TRUE | - | 복약 활성 여부 |
| started_at | DATE | Y |  | - | 복약 시작일 |
| end_at | DATE | Y |  | - | 복약 종료일 |
| created_at | DATETIME/TIMESTAMP | Y |  | - | 생성일 |
| updated_at | DATETIME/TIMESTAMP | Y |  | - | 수정일 |
| medication_id | BIGINT | - | - | deprecated | 제거됨 (`medication_dosage_id` 경유 참조) |
| hospital_id | BIGINT | - | - | deprecated | 제거됨 (진료 정보에 포함) |
| alarm_active | BOOLEAN | - | - | deprecated | 제거됨 (`consultations.alarm_settings` 사용) |
| quantity | DECIMAL | - | - | deprecated | 제거됨 (schedule 분리 구조에서 미사용) |

### 2.5 `user_medication_schedules`

| Column | Type | Null | Constraint | Change | 설명 |
|---|---|---|---|---|---|
| id | BIGINT | N | PK, AUTO_INCREMENT | - | 스케줄 ID |
| user_medication_id | BIGINT | N | FK -> `user_medications.id` | - | 사용자 복약 참조 |
| dose_time | TIME | N |  | - | 복용 시각 |
| label | VARCHAR(100) | Y |  | - | 라벨(아침/점심/저녁 등) |
| is_active | BOOLEAN | N | DEFAULT true | new | 활성 여부. 복용 시간 변경/중단 시 false(소프트 삭제, 로그 보존) |
| (user_medication_id, dose_time) | - | - | UNIQUE | new | 동일 복약 내 동일 시각 중복 방지 (재추가 시 기존 행 재활성화) |
| medication_strength_id | BIGINT | - | - | deprecated | 제거됨 |
| quantity | DECIMAL(4,2) | - | - | deprecated | 제거됨 |

### 2.6 `user_medication_logs`

| Column | Type | Null | Constraint | Change | 설명 |
|---|---|---|---|---|---|
| id | BIGINT | N | PK, AUTO_INCREMENT | - | 로그 ID |
| user_medication_schedule_id | BIGINT | N | FK -> `user_medication_schedules.id` | - | 스케줄 참조 |
| taken_at | DATETIME/TIMESTAMP | N |  | - | 복용 시각 |
| status | VARCHAR(50) | N | enum string | - | TAKEN/SKIPPED (missed는 저장하지 않고 분석에서 도출) |
| (user_medication_schedule_id, taken_at) | - | - | UNIQUE | - | 중복 로그 방지 |

## 3) 핵심 설계 원칙

- 복약 용량은 `medications`에 두지 않고 `medication_dosages`로 분리하여 정규화.
- `UserMedicationDose` 별도 엔티티는 두지 않고, `user_medications.medication_dosage_id`로 직접 참조.
- 스케줄은 "시간" 기준 책임만 가지며(`quantity` 없음), 용량 정보는 `user_medications`에 귀속.
- 병원/알림 맥락은 복약 엔티티가 아니라 `consultations`에서 관리.
