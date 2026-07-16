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
| user_status | VARCHAR(50) | DEFAULT PENDING (PENDING, ACTIVE, SUSPENDED, WITHDRAWAL, DELETED) | 계정 상태 |
| user_type | VARCHAR(50) | DEFAULT USER (USER, ADMIN) | 사용자 유형 (Enum) |
| onboarded_at | TIMESTAMP | | 온보딩 시점 |
| onboarding_skipped | BOOLEAN | DEFAULT false | 온보딩 전체 건너뜀 여부 |
| withdrawal_at | TIMESTAMP | | 탈퇴 요청 시점 (영구 삭제 배치 기준) |

---

## UserSetting (유저 설정)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| user_id | UUID | PK, FK → User.id, NOT NULL | 사용자 ID |
| medication_notification | BOOLEAN | DEFAULT true | 복약 알림 여부 |
| report_notification | BOOLEAN | DEFAULT true | 리포트 알림 여부 |
| marketing_notification | BOOLEAN | DEFAULT false | 마케팅 알림 여부 |
| community_notification | BOOLEAN | DEFAULT true | 커뮤니티 댓글 알림 여부 |
| todo_notification | BOOLEAN | DEFAULT true | Todo 마감 알림 여부 |
| take_medication_on_holiday | BOOLEAN | DEFAULT false | 휴일 복약 여부 |
| theme | VARCHAR(50) | DEFAULT SYSTEM (DARK, LIGHT, SYSTEM) | 테마 설정 |
| timezone | VARCHAR(64) | NOT NULL, DEFAULT Asia/Seoul, INDEX(idx_user_settings_timezone) | 사용자 최신 IANA timezone ID |

---

## Term (약관)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 약관 고유 식별자 |
| version | INTEGER | NOT NULL | 약관 버전 |
| type | VARCHAR(50) | NOT NULL | 약관 종류 Enum (TERMS_OF_SERVICE, PRIVACY_POLICY, MARKETING_CONSENT, AI_ANALYSIS_CONSENT) |
| content | TEXT | NOT NULL | 약관 내용 |
| effectiveAt | TIMESTAMP | NOT NULL | 약관 시행일 |
| createdAt | TIMESTAMP | NOT NULL | 생성일시 |

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
| name | VARCHAR(255) | NOT NULL, UNIQUE | 약물 상품명 (예: 콘서타OROS서방정, 메디키넷리타드캡슐) |
| genericName | VARCHAR(255) | | 성분명 (예: methylphenidate, atomoxetine) |
| effect | TEXT | | 약물 효과 |
| sideEffect | TEXT | | 약물 부작용 |
| graphUrl | TEXT | | 혈중 농도 그래프 URL |
| formulation | VARCHAR | | 제형 (예: IR 속방형, ER 서방형, OROS, 캡슐 등) |
| typical_dosage_range | VARCHAR | | 일반적인 용량 범위 (성인 기준) |
| drug_class | VARCHAR | | 약물 계열 한국어 표시명 (예: ADHD 자극제, ADHD 비자극제, SSRI 항우울제/항불안제) |
| description | TEXT | | 약별 특징·차이점·복용상 특징 |
| image_url | TEXT | | 약 이미지 URL |
| source_url | TEXT | | 공식 출처 링크 |

---

## UserMedication (사용자 약물)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 사용자 약물 고유 식별자 |
| user_id | UUID | FK → User.id, NOT NULL | 사용자 ID |
| medication_id | BIGINT | FK → Medication.id, NOT NULL | 약물 ID |
| hospital_id | BIGINT | FK → Hospital.id | 병원 ID |
| is_active | BOOLEAN | DEFAULT true | 복용 활성화 여부 |
| alarm_active | BOOLEAN | DEFAULT true | 알림 활성화 여부 |
| started_at | DATE | | 복용 시작일 |
| end_at | DATE | | 복용 종료일 |
| created_at | TIMESTAMP | NOT NULL | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL | 수정일시 |
| | | INDEX(idx_user_medications_user_active_created_id on user_id, is_active, created_at, id) | 사용자별 복약 목록 조회 정렬 지원 |
| | | INDEX(idx_user_medications_user_started_end on user_id, started_at, end_at) | 사용자별 복약 기간 겹침 조회 지원 |

---

## MedicationStrength (약물 용량 마스터)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 용량 고유 식별자 |
| medication_id | BIGINT | FK → Medication.id, NOT NULL | 약물 ID |
| amount | DECIMAL(6,2) | NOT NULL | 용량 값 (예: 18.00) |
| unit | VARCHAR(10) | NOT NULL | 용량 단위 (mg, mL 등) |
| form | VARCHAR(30) | | 제형 (tablet, capsule 등) |
| is_active | BOOLEAN | DEFAULT true | 사용 가능 여부 |
| | | UNIQUE(medication_id, amount, unit, form) | 동일 약물 내 용량 중복 방지 |

---

## UserMedicationSchedule (약물 복용 스케줄)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 스케줄 고유 식별자 |
| user_medication_id | BIGINT | FK → UserMedication.id, NOT NULL | 사용자 약물 ID |
| medication_strength_id | BIGINT | FK → MedicationStrength.id, NOT NULL | 약물 용량 ID |
| quantity | DECIMAL(4,2) | NOT NULL | 1회 복용 수량 (1정, 0.5정 등) |
| doseTime | TIME | NOT NULL | 복용 시간 |
| label | VARCHAR(100) | | 복용 레이블 (아침/점심/저녁 등) |
| is_active | BOOLEAN | NOT NULL, DEFAULT true | 활성 여부. 복용 시간 변경/중단 시 false로 소프트 삭제(복용 로그 보존을 위해 물리 삭제하지 않음) |
| | | UNIQUE(user_medication_id, dose_time) | 같은 복약 내 동일 복용 시간 중복 방지 (재추가 시 기존 행 재활성화로 충돌 회피) |
| | | INDEX(idx_user_medication_schedules_active_dose_time on is_active, dose_time) | timezone별 복약 알림 후보 조회 (동등 조건 is_active 선두) |

---

## UserMedicationLog (약물 복용 로그)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 약물 복용 로그 고유 식별자 |
| user_medication_schedule_id | BIGINT | FK → UserMedicationSchedule.id, NOT NULL | 사용자 약물 ID |
| taken_at | TIMESTAMP | NOT NULL | 복용 일시. 오프라인 재전송 시 클라이언트가 기록한 시각(`takenAt`)을 사용 |
| status | VARCHAR | NOT NULL | 복용 여부 enum값 (TAKEN, SKIPPED). 미복용(missed)은 로그를 남기지 않고 "예정 대비 기록 부재"로 분석에서 도출 |
| is_active | BOOLEAN | NOT NULL, DEFAULT true | 소프트 딜리트 플래그 (복용 취소 시 false로 변경) |
| active_dose_date | DATE | NULL 허용 | `(is_active, taken_at)`에서 파생. 활성이면 `DATE(taken_at)`, 비활성이면 NULL |
| | | UNIQUE(user_medication_schedule_id, active_dose_date) | 스케줄당 하루 활성 로그 1건 강제. MySQL은 유니크 인덱스에서 NULL을 서로 다른 값으로 취급하므로 취소된 로그는 같은 날 여러 건 공존 |

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

## JournalTag (태그 마스터)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT, NOT NULL | 태그 고유 식별자 |
| category | VARCHAR(30) | NOT NULL | CONDITION / SIDE_EFFECT / TROUBLE |
| name | VARCHAR(255) | NOT NULL | 태그 표시 이름 (NFC 정규화) |
| tag_type | VARCHAR(50) | NOT NULL | 세부 유형 (CALM / UP / DOWN / TIGHT / FOGGY / INATTENTION / TIME_MANAGEMENT / IMPULSIVITY / HYPERACTIVITY / COGNITIVE_ERROR / NONE) |
| scope | VARCHAR(20) | NOT NULL | SYSTEM / USER |
| owner_user_id | BINARY(16) | NULL | USER 태그 소유자 UUID. SYSTEM 태그는 NULL |
| owner_key | BINARY(16) | NOT NULL | USER: owner_user_id 와 동일. SYSTEM: 0x00...00 (SYSTEM_OWNER_KEY) |
| is_active | TINYINT(1) | NOT NULL | 활성 여부. USER 태그 삭제 시 false 소프트 삭제 |
| default_visible | TINYINT(1) | NOT NULL | preference 미설정 사용자의 기본 노출 여부 |
| created_at | DATETIME(6) | NOT NULL | 생성 시각 |
| updated_at | DATETIME(6) | NOT NULL | 최종 수정 시각 |
| | | UNIQUE(owner_key, category, name, tag_type) `uq_journal_tags_owner_category_name_type` | 소유자 내 중복 태그 방지 |

---

## UserJournalTagPreference (사용자 태그 표시 설정)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| user_id | BINARY(16) | PK, FK → User.id ON DELETE CASCADE, NOT NULL | 사용자 UUID |
| journal_tag_id | BIGINT | PK, FK → JournalTag.id ON DELETE CASCADE, NOT NULL | 태그 ID |
| is_enabled | TINYINT(1) | NOT NULL | 이 태그를 체크인에서 사용할지 여부 |
| is_visible | TINYINT(1) | NOT NULL | 체크인 화면에 노출할지 여부 |
| updated_at | DATETIME(6) | NOT NULL | 최종 수정 시각 |

---

## JournalTagLog (통합 태그 체크 로그)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT, NOT NULL | 로그 고유 식별자 |
| user_id | UUID | FK → User.id ON DELETE CASCADE, NOT NULL | 사용자 ID |
| journal_tag_id | BIGINT | FK → JournalTag.id ON DELETE RESTRICT, NOT NULL | 태그 ID |
| journal_date | DATE | NOT NULL | 체크인 대상 날짜 (사용자 최신 timezone 기준) |
| checked_at | TIMESTAMP | NOT NULL | 체크인 서버 수신 시각 (Asia/Seoul) |
| | | UNIQUE(user_id, journal_tag_id, journal_date) | 동일 날짜 중복 체크 방지 |
| | | INDEX(idx_journal_tag_logs_user_date_checked on user_id, journal_date, checked_at) | 사용자별 기간 조회 및 날짜/체크 시각 정렬 지원 |

---

## DailyGoal (일일 목표)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 일일 목표 고유 식별자 |
| user_id | UUID | FK → User.id, NOT NULL | 사용자 ID |
| dailyGoal | VARCHAR(500) | NOT NULL | 일일 목표 내용 |
| isActive | BOOLEAN | DEFAULT true | 목표 활성화 여부 |
| savedAt | TIMESTAMP | NULLABLE | 최초 생성 시각 |
| | | UNIQUE(user_id, dailyGoal) | 같은 일일 목표 중복 생성 방지 |

---

## OnboardingGoalSnapshot (온보딩 회차별 목표 스냅샷)

온보딩 회차마다 선택된 목표를 기록하여 이력 조회 시 회차별 목표를 정확하게 재현한다.
동일 목표가 여러 회차에 재사용되더라도 각 회차의 이력이 독립적으로 보존된다.

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 스냅샷 고유 식별자 |
| user_id | UUID | NOT NULL | 사용자 ID |
| daily_goal_id | BIGINT | FK → DailyGoal.id, NOT NULL | 해당 온보딩 회차에 선택된 목표 |
| onboardingTime | TIMESTAMP | NOT NULL | 온보딩 목표 저장 시각 (회차 구분용) |

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
| post_category | VARCHAR(50) | NOT NULL | 게시글 카테고리 (Enum) |
| title | VARCHAR(255) | NOT NULL | 제목 |
| content | TEXT | NOT NULL | 내용 |
| created_at | TIMESTAMP | NOT NULL | 생성일시 |
| updated_at | TIMESTAMP | | 수정일시 |
| is_anonymous | BOOLEAN | DEFAULT false | 익명 여부 |
| is_deleted | BOOLEAN | DEFAULT false | 소프트 삭제 여부 |
| | | INDEX(idx_community_boards_deleted_created on created_at, is_deleted) | 삭제되지 않은 게시글 최신순 목록 조회 지원 |
| | | INDEX(idx_community_boards_deleted_category_created on is_deleted, post_category, created_at) | 카테고리 필터 게시글 최신순 목록 조회 지원 |

---

## Comment (댓글)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 댓글 고유 식별자 |
| user_id | UUID | FK → User.id, NOT NULL | 작성자 ID |
| post_id | BIGINT | FK → CommunityBoard.id, NOT NULL | 게시글 ID |
| is_anonymous | BOOLEAN | DEFAULT false | 익명 여부 |
| content | TEXT | NOT NULL | 댓글 내용 |
| is_deleted | BOOLEAN | DEFAULT false | 소프트 삭제 여부 |
| created_at | TIMESTAMP | NOT NULL | 댓글 생성 일자 |
| updated_at | TIMESTAMP | | 댓글 수정 일자 |
| | | INDEX(idx_comments_post_deleted_created on post_id, is_deleted, created_at) | 게시글별 삭제되지 않은 댓글 생성순 조회 지원 |

---

## Consultation (상담 기록)

테이블명: `consultations`

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 상담 기록 고유 식별자 |
| user_id | UUID | FK → User.id, NOT NULL | 사용자 ID |
| consultation_date | TIMESTAMP | NOT NULL | 상담일시 |
| place | VARCHAR(255) | NOT NULL | 상담 장소 |
| doctor_name | VARCHAR(255) | | 의사 이름 |
| is_first_visit | BOOLEAN | NOT NULL | 첫 상담 여부 |
| summary_report | TEXT | | 상담 요약 리포트 |
| doctor_advice | TEXT | | 의사 조언 |
| prescription_note | TEXT | | 처방 메모 |
| next_treatment_goal | TEXT | | 다음 치료 목표 |
| alarm_settings | BOOLEAN | NOT NULL, DEFAULT true | 알림 설정 여부 |
| is_deleted | BOOLEAN | NOT NULL | 소프트 삭제 여부 |
| created_at | TIMESTAMP | | 생성 일시 |
| updated_at | TIMESTAMP | | 수정 일시 |
| | | INDEX(idx_consultations_user_deleted_date on user_id, is_deleted, consultation_date) | 사용자별 상담 기간 조회 정렬 지원 |

---

## ConsultationQuestion (상담 전 질문)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 질문 고유 식별자 |
| consultation_id | BIGINT | FK → Consultation.id, NOT NULL | 상담 기록 ID |
| text | VARCHAR(255) | NOT NULL | 질문 내용 |
| createdAt | TIMESTAMP | | 생성 일시 |

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
| external_event_id | VARCHAR(255) | | 외부 일정 ID |
| external_provider | VARCHAR(50) | | 외부 제공자 |
| place | VARCHAR(500) | | 장소 |
| is_all_day | BOOLEAN | DEFAULT false | 종일 일정 여부 |
| start_time | TIMESTAMP | NOT NULL | 시작일시 |
| end_time | TIMESTAMP | NOT NULL | 종료일시 |
| alarm_enabled | BOOLEAN | DEFAULT false | 알림 사용 여부 |
| is_deleted | BOOLEAN | DEFAULT false | 소프트 삭제 여부 |
| | | INDEX(idx_schedules_user_deleted_start_end on user_id, is_deleted, start_time, end_time) | 사용자별 기간 일정 조회 지원 |

---

## ScheduleAlarm (일정 알람 시각)

알림 시각을 별도 테이블로 분리하여 인덱스 기반 조회 지원.

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 알람 고유 식별자 |
| schedule_id | BIGINT | FK → Schedule.id, NOT NULL | 일정 ID |
| alarm_at | DATETIME(6) | NOT NULL, INDEX(idx_schedule_alarm_sent_at with is_sent) | 알람 발송 예정 시각 |
| is_sent | BOOLEAN | NOT NULL, DEFAULT false, INDEX(idx_schedule_alarm_sent_at with alarm_at) | 알람 발송 완료 여부 |
| fail_count | TINYINT | NOT NULL, DEFAULT 0 | 발송 실패 횟수 (3회 이상 시 isSent=true로 포기 처리) |

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
| selected_symptom_types | VARCHAR(255) | | 직접 체크한 증상 유형 (쉼표 구분) |
| selected_functional_areas | VARCHAR(255) | | 직접 체크한 취약 기능 영역 (쉼표 구분) |
| is_quick_onboarding | BOOLEAN | NOT NULL, DEFAULT false | 퀵 온보딩 경로 여부 |
| | | INDEX(idx_onboarding_symptoms_user_saved on user_id, saved_at) | 사용자별 최신/시점 기준 온보딩 증상 조회 지원 |

---

## SupportInquiry (고객 문의)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 고유 식별자 |
| user_id | UUID | FK → User.id, NOT NULL | 작성자 ID |
| type | VARCHAR(50) | NOT NULL | 문의 유형 Enum (BUG / FEATURE / USAGE / PAYMENT / OTHER) |
| title | VARCHAR(100) | NOT NULL | 제목 |
| content | TEXT | NOT NULL | 문의 내용 |
| email | VARCHAR(255) | NOT NULL | 연락처 이메일 |
| status | VARCHAR(50) | DEFAULT PENDING | 처리 상태 (PENDING / IN_PROGRESS / RESOLVED) |
| created_at | TIMESTAMP | NOT NULL | 생성일시 |

---

## Todo (할 일)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 할 일 고유 식별자 |
| user_id | UUID | FK → User.id, NOT NULL | 사용자 ID |
| text | VARCHAR(100) | NOT NULL | 할 일 내용 |
| due_at | TIMESTAMP | NOT NULL | 마감일시 |
| is_all_day | BOOLEAN | DEFAULT false | 종일 여부 |
| is_completed | BOOLEAN | DEFAULT false | 완료 여부 |
| is_deleted | BOOLEAN | DEFAULT false | 소프트 삭제 여부 |
| is_alarm_sent | BOOLEAN | DEFAULT false | 알람 발송 완료 여부 (SENT/SKIPPED 시 true로 갱신) |
| created_at | TIMESTAMP | NOT NULL | 생성일시 |
| | | INDEX(idx_todos_alarm_lookup on is_alarm_sent, is_deleted, is_completed, is_all_day, due_at) | 할 일 알림 후보 조회 지원 |
| | | INDEX(idx_todos_user_deleted_due_at on user_id, is_deleted, due_at) | 사용자별 날짜 범위 할 일 조회 지원 |

---

## NotificationSubscription (푸시 알람 구독 정보)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 구독 고유 식별자 |
| user_id | UUID | FK → User.id, NOT NULL | 사용자 ID |
| platform | VARCHAR(20) | NOT NULL | 플랫폼 Enum (WEB, ANDROID, IOS) |
| provider | VARCHAR(20) | NOT NULL | 발송 채널 Enum (WEB_PUSH, FCM, APNS) |
| endpoint | VARCHAR(2048) CHARACTER SET ascii | | Web Push endpoint URL |
| p256dh | VARCHAR(500) | | Web Push 공개키 |
| auth | VARCHAR(255) | | Web Push 인증 시크릿 |
| token | VARCHAR(2048) CHARACTER SET ascii | | FCM / APNs 토큰 |
| enabled | BOOLEAN | NOT NULL | 활성화 여부 |
| created_at | TIMESTAMP | NOT NULL | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL | 수정일시 |
| | | UNIQUE(user_id, endpoint) | Web Push 중복 방지 |
| | | UNIQUE(user_id, token) | FCM/APNs 중복 방지 |

---

## NotificationHistory (알람 발송 이력)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 이력 고유 식별자 |
| user_id | UUID | FK → User.id, NOT NULL | 수신 사용자 ID |
| alarm_type | VARCHAR(50) | NOT NULL | 알람 유형 Enum (MEDICATION, SCHEDULE, TODO, REPORT, COMMUNITY, MARKETING) |
| reference_id | BIGINT | NOT NULL, DEFAULT 0 | 연관 엔티티 ID (schedule_id, todo_id 등). reference 없는 알림(마케팅 등)은 0 사용 |
| alarm_scheduled_at | TIMESTAMP | NOT NULL | 예정 발송 시각 (중복 방지 기준) |
| title | VARCHAR(255) | NOT NULL | 알람 제목 |
| body | TEXT | | 알람 본문 |
| status | VARCHAR(20) | NOT NULL | 발송 상태 Enum (SENDING, SENT, FAILED, SKIPPED) |
| sent_at | TIMESTAMP | NOT NULL | 최근 발송 선점/시도 시각 |
| | | UNIQUE(user_id, alarm_type, reference_id, alarm_scheduled_at) | 중복 발송 방지 |



---

## MedicationAnalysisReport (약물 치료 경과 리포트)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | 리포트 고유 식별자 |
| user_id | UUID | FK → User.id, ON DELETE CASCADE, NOT NULL | 사용자 ID |
| period_start | DATE | NOT NULL | 분석 기간 시작일 |
| period_end | DATE | NOT NULL | 분석 기간 종료일 |
| status | VARCHAR(20) | NOT NULL | 리포트 상태 Enum (PENDING, COMPLETED, FAILED, OUTDATED) |
| source_data_hash | VARCHAR(64) | NOT NULL | 원본 데이터 SHA-256 해시 (중복 생성 방지 및 OUTDATED 감지) |
| row_count_hash | VARCHAR(64) | NULLABLE | 복약 데이터 건수 기반 해시 (OUTDATED 판단용) |
| snapshot_json | TEXT | | 서버 분석 스냅샷 JSON |
| ai_result_json | TEXT | | Gemini AI 분석 결과 JSON (미동의 또는 실패 시 NULL) |
| model_name | VARCHAR(100) | | 사용된 AI 모델명 |
| prompt_version | VARCHAR(20) | | 사용된 프롬프트 버전 |
| generated_at | TIMESTAMP | NOT NULL | 리포트 생성 시각 |
| | | INDEX(idx_medication_analysis_reports_user_generated on user_id, generated_at) | 사용자별 리포트 최신순 목록 조회 지원 |
| | | INDEX(idx_medication_analysis_reports_user_period on user_id, period_start, period_end) | 사용자별 리포트 기간 조회 지원 |

---

## AdminAuditLog (관리자 감사 로그)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | UUID | PK, NOT NULL | 감사 로그 ID |
| action | VARCHAR(40) | NOT NULL | 감사 액션 |
| target_reference | VARCHAR(128) | NOT NULL | 대상 참조값 |
| target_label | VARCHAR(200) | | 대상 표시명 |
| admin_id | UUID | NOT NULL | 관리자 ID |
| admin_email | VARCHAR(320) | NOT NULL | 관리자 이메일 |
| reason | VARCHAR(1000) | NOT NULL | 처리 사유 |
| created_at | TIMESTAMP | NOT NULL | 생성 시각 |
| | | INDEX(idx_admin_audit_logs_created_id on created_at, id) | 최신 감사 로그 조회 정렬 지원 |

---

## AsrsAssessment (ASRS 평가)

| Column Name | DB Data Type | Constraints | Description |
|---|---|---|---|
| id | BIGINT | PK, NOT NULL | ASRS 평가 ID |
| user_id | UUID | FK → User.id | 사용자 ID |
| partascore | INTEGER | NOT NULL | Part A 점수 |
| total_score | INTEGER | NOT NULL | 총점 |
| completed_at | TIMESTAMP | | 완료 시각 |
| | | INDEX(idx_asrs_assessments_user_completed on user_id, completed_at) | 사용자별 ASRS 최신/다음 평가 조회 지원 |

