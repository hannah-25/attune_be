# DB Schema

## User

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | UUID | PK, NOT NULL | 사용자 고유 식별자 |
| email | VARCHAR(255) | NOT NULL, UNIQUE | 이메일 |
| nickname | VARCHAR(100) | NOT NULL, UNIQUE | 닉네임 |
| password | VARCHAR(255) | | 비밀번호 |
| profile_image_url | TEXT | | 프로필 이미지 URL |
| provider | VARCHAR(50) | | 소셜 로그인 제공자 |
| provider_id | VARCHAR(100) | | 소셜 로그인 provider id |
| user_status | VARCHAR(50) | DEFAULT PENDING (PENDING, ACTIVE, SUSPENDED, WITHDRAWAL) | 계정 상태 |
| user_type | VARCHAR(50) | DEFAULT USER (USER, ADMIN) | 사용자 유형 (Enum) |
| onboarded_at | TIMESTAMP | | 온보딩 시점 |
| withdrawal_at | TIMESTAMP | | 탈퇴 요청 시점 (영구 삭제 배치 기준) |

---

## UserSetting (유저 설정)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| user_id | UUID | PK, FK → User.id, NOT NULL | 사용자 ID |
| medication_notification | BOOLEAN | DEFAULT true | 복약 알림 여부 |
| report_notification | BOOLEAN | DEFAULT true | 리포트 알림 여부 |
| marketing_notification | BOOLEAN | DEFAULT false | 마케팅 알림 여부 |
| take_medication_on_holiday | BOOLEAN | DEFAULT false | 휴일 복약 여부 |
| theme | VARCHAR(50) | DEFAULT SYSTEM (DARK, LIGHT, SYSTEM) | 테마 설정 |

---

## Term (약관)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 약관 고유 식별자 |
| version | INTEGER | NOT NULL | 약관 버전 |
| content | TEXT | NOT NULL | 약관 내용 |
| effectiveAt | TIMESTAMP | NOT NULL | 약관 시행일 |
| createdAt | TIMESTAMP | NOT NULL | 생성일시 |
| marketing_consent | TEXT | | |
| privacy_policy | TEXT | | |
| terms_of_service | TEXT | | |

---

## UserTermAgreement (약관 동의)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 약관 동의 고유 식별자 |
| user_id | UUID | FK → User.id, NOT NULL | 사용자 ID |
| terms_id | BIGINT | FK → Terms.id, NOT NULL | 약관 ID |
| notifiedAt | BOOLEAN | DEFAULT true | 약관 변경 알림 여부 |
| isAgreed | BOOLEAN | DEFAULT false → 변경 알림 후 30일 후 true | 동의 여부 |
| agreedAt | TIMESTAMP | DEFAULT false → notifiedAt + 30일 | 동의일시 |


---

## Hospital (병원)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 병원 고유 식별자 |
| doctor | VARCHAR(100) | | 의사 이름 |
| hospitalName | VARCHAR(255) | NOT NULL | 병원 이름 |
| hospitalAddress | VARCHAR(500) | | 병원 주소 |
| | | UNIQUE(hospitalName, hospitalAddress) | 같은 이름과 주소의 병원 중복 등록 방지 |

---

## Medication (약물)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 약물 고유 식별자 |
| name | VARCHAR(255) | NOT NULL, UNIQUE | 약물 상품명 |
| genericName | VARCHAR(255) | | 약물 일반명 |
| effect | TEXT | | 약물 효과 |
| sideEffect | TEXT | | 약물 부작용 |
| graphUrl | TEXT | | 약물 그래프 URL |
| formulation | VARCHAR | | 제형 (예: IR 속방형, ER 서방형, OROS, 캡슐 등) |
| typical_dosage_range | VARCHAR | | 일반적인 용량 범위 (성인 기준) |
| drug_class | VARCHAR | | 약물 계열 (stimulant, non-stimulant) |

---

## UserMedication (사용자 약물)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 사용자 약물 고유 식별자 |
| user_id | UUID | FK → User.id, NOT NULL | 사용자 ID |
| medication_id | BIGINT | FK → Medication.id, NOT NULL | 약물 ID |
| hospital_id | BIGINT | FK → Hospital.id | 병원 ID |
| isActive | BOOLEAN | DEFAULT true | 복용 활성화 여부 |
| alarmActive | BOOLEAN | DEFAULT true | 알림 활성화 여부 |
| startedAt | DATE | | 복용 시작일 |
| endAt | DATE | | 복용 종료일 |

---

## UserMedicationSchedule (약물 복용 스케줄)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 약물 복용 스케줄 고유 식별자 |
| user_medication_id | BIGINT | FK → UserMedication.id, NOT NULL | 사용자 약물 ID |
| doseTime | TIME | NOT NULL | 복용 시간 |
| label | VARCHAR(100) | | 복용 라벨 (아침, 점심, 저녁 등) |
| dosage | VARCHAR(50) | NOT NULL | 용량 |

---

## UserMedicationLog (약물 복용 로그)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 약물 복용 로그 고유 식별자 |
| user_medication_schedule_id | BIGINT | FK → UserMedicationSchedule.id, NOT NULL | 사용자 약물 ID |
| takenAt | TIMESTAMP | NOT NULL | 복용 일시 |
| | | UNIQUE(id, takenAt) | 같은 약물을 같은 시간에 중복 복용 로그 방지 |
| status | VARCHAR | NOT NULL | 복용 여부 enum값 (skipped, missed, taken) |

---

## ConditionTag (컨디션 태그)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 컨디션 태그 고유 식별자 |
| user_id | UUID | FK → User.id, NOT NULL | 사용자 ID |
| condition | VARCHAR(255) | NOT NULL | 컨디션 내용 |
| type | VARCHAR(50) | NOT NULL | 컨디션 유형 (Enum) |
| isActive | BOOLEAN | DEFAULT true | 태그 활성화 여부 |

---

## ConditionLog (컨디션 로그)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 컨디션 로그 고유 식별자 |
| condition_tag_id | BIGINT | FK → ConditionTag.id, NOT NULL | 컨디션 태그 ID |
| checkedAt | TIMESTAMP | NOT NULL | 체크 일시 |

---

## SideEffectTag (부작용 태그)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 부작용 태그 고유 식별자 |
| user_id | UUID | FK → User.id, NOT NULL | 사용자 ID |
| sideEffect | VARCHAR(255) | NOT NULL | 부작용 내용 |
| isActive | BOOLEAN | DEFAULT true | 태그 활성화 여부 |

---

## SideEffectLog (부작용 로그)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 부작용 로그 고유 식별자 |
| side_effect_tag_id | BIGINT | FK → SideEffectTag.id, NOT NULL | 부작용 태그 ID |
| checkedAt | TIMESTAMP | NOT NULL | 체크 일시 |

---

## DailyStatusLog (일일 상태 로그)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 일일 상태 로그 고유 식별자 |
| user_id | UUID | FK → User.id, NOT NULL | 사용자 ID |
| sleepHour | FLOAT | | 수면 시간 |
| sleepQuality | VARCHAR(50) | | 수면 질 (Enum) |
| ateBreakfast | BOOLEAN | | 아침식사 여부 |
| ateLunch | BOOLEAN | | 점심식사 여부 |
| ateDinner | BOOLEAN | | 저녁식사 여부 |
| date | DATE | NOT NULL | 날짜 |
| | | UNIQUE(user_id, date) | 사용자별 날짜 중복 방지 |

---

## TroubleTag (트러블 태그)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 트러블 태그 고유 식별자 |
| user_id | UUID | FK → User.id, NOT NULL | 사용자 ID |
| trouble | VARCHAR(255) | NOT NULL | 트러블 내용 |
| type | VARCHAR(50) | NOT NULL | 트러블 유형 (Enum) |
| isActive | BOOLEAN | DEFAULT true | 태그 활성화 여부 |

---

## TroubleLog (일일 트러블 로그)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 일일 트러블 로그 고유 식별자 |
| trouble_tag_id | BIGINT | FK → TroubleTag.id, NOT NULL | 트러블 태그 ID |
| checkedAt | TIMESTAMP | NOT NULL | 체크 일시 |

---

## DailyGoal (일일 목표)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 일일 목표 고유 식별자 |
| user_id | UUID | FK → User.id, NOT NULL | 사용자 ID |
| dailyGoal | VARCHAR(500) | NOT NULL | 일일 목표 내용 |
| isActive | BOOLEAN | DEFAULT true | 목표 활성화 여부 |
| | | UNIQUE(user_id, dailyGoal) | 같은 일일 목표 중복 생성 방지 |

---

## DailyGoalLog (일일 목표 달성 로그)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 일일 목표 달성 로그 고유 식별자 |
| daily_goal_achievement_id | BIGINT | FK → DailyGoalAchievement.id, NOT NULL | 일일 목표 ID |
| score | INTEGER | | 달성 점수 |
| date | DATE | NOT NULL | 날짜 |
| | | UNIQUE(daily_goal_achievement_id, date) | 같은 목표에 같은 날짜 중복 로그 방지 |

---

## Notice (공지사항)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 공지사항 고유 식별자 |
| title | VARCHAR(255) | NOT NULL | 제목 |
| content | TEXT | NOT NULL | 내용 |
| pushEnabled | BOOLEAN | DEFAULT false | 푸시 알림 여부 |
| createdAt | TIMESTAMP | NOT NULL | 생성일시 |

---

## CommunityBoard (커뮤니티 게시판)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 커뮤니티 게시판 고유 식별자 |
| user_id | UUID | FK → User.id, NOT NULL | 작성자 ID |
| postCategory | VARCHAR(50) | NOT NULL | 게시글 카테고리 (Enum) |
| title | VARCHAR(255) | NOT NULL | 제목 |
| content | TEXT | NOT NULL | 내용 |
| createdAt | TIMESTAMP | NOT NULL | 생성일시 |
| updatedAt | TIMESTAMP | | 수정일시 |
| isAnonymous | BOOLEAN | DEFAULT false | 익명 여부 |
| isDeleted | BOOLEAN | DEFAULT false | 소프트 삭제 여부 |

---

## Comment (댓글)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 댓글 고유 식별자 |
| user_id | UUID | FK → User.id, NOT NULL | 작성자 ID |
| community_board_id | BIGINT | FK → CommunityBoard.id, NOT NULL | 게시글 ID |
| isAnonymous | BOOLEAN | DEFAULT false | 익명 여부 |
| content | TEXT | NOT NULL | 댓글 내용 |
| isDeleted | BOOLEAN | DEFAULT false | 소프트 삭제 여부 |
| createdAt | TIMESTAMP | NOT NULL | 댓글 생성 일자 |
| updatedAt | TIMESTAMP | | 댓글 수정 일자 |

---

## ConsultationRecord (상담 기록)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 상담 기록 고유 식별자 |
| user_id | UUID | FK → User.id, NOT NULL | 사용자 ID |
| hospital_id | BIGINT | FK → Hospital.id, NOT NULL | 병원 ID |
| date | TIMESTAMP | NOT NULL | 상담일시 |
| isFirst | BOOLEAN | DEFAULT false | 첫 상담 여부 |
| preConsultationNote | TEXT | | 상담 전 메모 |
| nextTreatmentGoal | TEXT | | 다음 치료 목표 |
| doctorAdvice | TEXT | | 의사 조언 |
| prescriptionNote | TEXT | | 처방 메모 |

---

## ExternalCalendarAccount (외부 캘린더 계정)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 외부 캘린더 계정 고유 식별자 |
| user_id | UUID | FK → User.id, NOT NULL | 사용자 ID |
| provider | VARCHAR(50) | NOT NULL | 캘린더 제공자 |
| externalUserId | VARCHAR(255) | NOT NULL | 외부 사용자 ID |
| accessToken | TEXT | NOT NULL | 액세스 토큰 |
| refreshToken | TEXT | | 리프레시 토큰 |
| tokenExpiredAt | TIMESTAMP | | 토큰 만료일시 |
| | | UNIQUE(user_id, provider) | 사용자별 제공자 중복 방지 |

---

## ScheduleCategory (일정 카테고리)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 일정 카테고리 고유 식별자 |
| user_id | UUID | FK → User.id, NOT NULL | 사용자 ID |
| color | VARCHAR(20) | NOT NULL | 카테고리 색상 |
| categoryName | VARCHAR(100) | NOT NULL | 카테고리 이름 |
| | | UNIQUE(user_id, categoryName) | 같은 사용자에게 같은 이름의 카테고리 중복 방지 |

---

## Schedule (일정)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 일정 고유 식별자 |
| user_id | UUID | FK → User.id, NOT NULL | 사용자 ID |
| schedule_category_id | BIGINT | FK → ScheduleCategory.id | 일정 카테고리 ID |
| title | VARCHAR(255) | NOT NULL | 일정 제목 |
| description | TEXT | | 일정 설명 |
| externalEventId | VARCHAR(255) | | 외부 일정 ID |
| externalProvider | VARCHAR(50) | | 외부 제공자 |
| place | VARCHAR(500) | | 장소 |
| isAllDay | BOOLEAN | DEFAULT false | 종일 일정 여부 |
| startTime | TIMESTAMP | NOT NULL | 시작일시 |
| endTime | TIMESTAMP | NOT NULL | 종료일시 |
| alarmEnabled | BOOLEAN | DEFAULT false | 알림 여부 |
| alarmedAt | TIMESTAMP | | 알림 발송일시 |
| isActive | BOOLEAN | DEFAULT true | 일정 활성화 여부 |

---

## Report (리포트)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 리포트 고유 식별자 |
| user_id | UUID | FK → User.id, NOT NULL | 사용자 ID |
| date | DATE | NOT NULL | 리포트 날짜 |
| | | UNIQUE(id, date) | 일자당 하나의 일지만 생성 |
| analysis | TEXT | NOT NULL | |

---

## OnboardingSymptom (온보딩 증상)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 온보딩 증상 고유 식별자 |
| user_id | UUID | FK → User.id, NOT NULL | 사용자 ID |
| description | TEXT | | 초기 증상 서술 |
| emotional_event | TEXT | | 감정적 사건 서술 |
| saved_at | TIMESTAMP | | 저장 일시 |