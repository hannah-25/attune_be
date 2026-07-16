# 실행 계획: 여행 중 현지시간 기준 복약 처리 일관화

- 상태: active
- 작성자 / 날짜: Codex / 2026-07-16
- 관련 이슈/PR: 미정

## 목표

사용자가 출장이나 여행으로 timezone을 변경했을 때 기존 복약 목표 시각(`doseTime`)을 현재 체류지의 현지 벽시계 시각으로 해석한다.
복약 알림, 복약 기록의 날짜, API 응답, 중복 방지가 동일한 timezone 정책을 사용하도록 정리한다.

예를 들어 `doseTime=09:00`인 사용자가 서울에서 뉴욕으로 이동해 timezone을 `America/New_York`으로 갱신하면,
이후 알림은 뉴욕 오전 9시에 발송되고 복약 기록도 뉴욕 현지 날짜에 귀속되어야 한다.

## 배경

현재 `user_settings.timezone`에 사용자의 최신 IANA timezone ID를 저장하고, 복약 알림 스케줄러는 `Instant`를 timezone별
현지 시각으로 변환하여 후보를 조회한다. 이 부분은 여행 중 현지시간을 따르는 제품 정책과 대체로 일치한다.

반면 복약 기록은 클라이언트가 보낸 `Instant`를 고정 `Asia/Seoul`의 `LocalDateTime`으로 변환해 저장하며, 일부 응답도
고정 `+09:00` offset을 붙인다. 따라서 해외 사용자는 알림은 현지시간에 받지만 복약일과 조회 결과는 KST 날짜로 계산될 수 있다.
알림 이력의 예약 시각도 timezone 없는 `LocalDateTime`이라 timezone 변경과 DST 중복 시각을 명확히 식별하기 어렵다.

## 확정 정책

- 복약 목표 시각 `doseTime`은 timezone이 없는 현지 벽시계 시각이다.
- 복약 알림은 항상 사용자의 최신 `user_settings.timezone`을 따른다.
- `homeTimezone`, 여행 모드, 약별 timezone 정책은 도입하지 않는다.
- 실제 복약 시각과 실제 알림 예정 시각은 절대 시각으로 관리한다.
- 복약일은 실제 복약 순간을 당시 적용 timezone으로 변환해 계산한다.
- 과거 기록은 사용자가 나중에 timezone을 바꾸더라도 날짜와 의미가 변경되지 않아야 한다.
- DST gap은 해당 날짜의 다음 유효 시각에 1회 발송하고, DST overlap은 앞선 offset에서 1회만 발송한다.
- 서버가 시차에 따른 의학적 복용 간격을 임의로 조정하지 않는다.

## 현재 상태

### 전역 시간 기준

- 애플리케이션, Gradle 실행 및 Docker JVM 기본 timezone은 `Asia/Seoul`이다.
- 다수 엔티티와 API가 timezone 없는 `LocalDateTime`을 사용한다.
- `hibernate.jdbc.time_zone`과 DB session timezone은 애플리케이션 공통 설정으로 강제되지 않는다.

### 사용자 timezone

- `UserSetting.DEFAULT_TIMEZONE`은 `Asia/Seoul`이다.
- 사용자 설정 PATCH에서 `ZoneId.of(timezone)`으로 IANA ID를 검증한다.
- `UserZoneResolver`가 설정 누락 또는 비정상 값을 `Asia/Seoul`로 폴백한다.

### 복약 알림

- `MedicationAlarmScheduler`는 `Instant.now()`를 각 활성 사용자 timezone의 현지 시각으로 변환한다.
- timezone별 현지 `LocalTime`과 복구 윈도우로 알림 후보를 조회한다.
- 알림 이력에는 사용자 현지 `LocalDateTime`인 `alarmScheduledAt`이 저장된다.

### 복약 기록

- `QuickLogRequest.takenAt`은 `Instant`이다.
- `MedicationService`가 이를 고정 `Asia/Seoul` `LocalDateTime`으로 바꿔 `UserMedicationLog.takenAt`에 저장한다.
- 활성 복약일 유일성은 `DATE(taken_at)`에서 파생된 `active_dose_date`에 의존한다.
- 기간 조회 응답은 저장된 `LocalDateTime`에 고정 `Asia/Seoul` offset을 붙인다.

## 변경 범위

### 사용자 timezone 적용

- 기존 사용자 설정 조회/PATCH API와 `UserZoneResolver`를 유지한다.
- 복약 도메인의 timezone 계산은 공용 애플리케이션 컴포넌트로 집중한다.
- `Clock`을 주입하여 현재 절대 시각과 사용자 현지 날짜를 재현 가능하게 계산한다.

### 복약 기록 데이터

- 복약 기록에 실제 복약 절대 시각을 저장한다.
- 복약 기록에 당시 현지 복약일과 적용 IANA timezone을 저장한다.
- 활성 복약일 유일 제약은 `taken_at`의 서버 날짜가 아닌 명시적인 현지 복약일을 사용한다.
- 기존 KST 기록의 의미를 보존하는 데이터 마이그레이션을 제공한다.

목표 논리 필드:

| 필드 | 의미 |
|---|---|
| `taken_at` | 실제 복약 순간. UTC 기준 절대 시각 |
| `dose_date` | 실제 복약 순간을 당시 사용자 timezone으로 변환한 날짜 |
| `dose_timezone` | `dose_date` 계산에 사용한 IANA timezone ID |
| `active_dose_date` | 활성 로그일 때만 `dose_date`, 취소 로그는 `NULL` |

물리 타입과 `TIMESTAMP`/`DATETIME(6)` 선택은 현재 MySQL/JDBC 동작을 통합 테스트로 확인한 후 확정한다.

### 알림 중복 방지

- 복약 알림의 현지 날짜와 `doseTime`, 사용자 timezone으로 실제 예정 `Instant`를 계산한다.
- 복약 알림 중복 식별은 timezone 없는 현지 시각이 아닌 절대 예정 시각을 사용한다.
- 감사와 장애 분석을 위해 계산 당시 timezone을 함께 보존하는 방안을 적용한다.
- timezone 변경 직후 이미 복용 완료했거나 이미 처리한 예정 시각을 다시 발송하지 않는다.

### API

- 복약 기록 응답의 실제 시각은 `Instant` 또는 offset이 포함된 ISO-8601 값으로 제공한다.
- 일별 그룹과 중복 판정에는 서버가 확정한 `doseDate`를 제공한다.
- 기록 당시 `doseTimezone`을 제공한다.
- 기존 응답 필드의 의미 또는 형식이 바뀌는 경우 즉시 교체하지 않고 신규 필드를 추가한 뒤 프론트 전환 후 구 필드를 제거한다.

## 제외 범위

- 일정, Todo, 상담, 커뮤니티 등 전체 도메인의 UTC 마이그레이션
- 일정 및 Todo 알림의 사용자별 timezone 적용
- Google Calendar 원본 timezone 보존 구조 변경
- `homeTimezone` 또는 여행 시작·종료 기간 저장
- 약별 `FOLLOW_LOCAL_TIME`/`KEEP_HOME_TIME` 정책
- 복약 간격에 대한 의학적 자동 보정
- 사용자 위치 또는 GPS 기반 timezone 추론
- 프론트엔드 구현 자체. 단, API 전환 계약과 필요한 동기화 요구사항은 문서화한다.

## 관련 문서

- `docs/global-time-alarm-spec.md`
- `docs/database.md`
- `docs/db_schema.md`
- `docs/api-guide.md`
- `docs/architecture/data-rules.md`
- `docs/agent/feature-workflow.md`
- `docs/agent/pr-review-checklist.md`

## 관련 코드

- `src/main/java/attune/user/domain/model/UserSetting.java:19`
- `src/main/java/attune/user/application/UserSettingService.java:32`
- `src/main/java/attune/user/application/UserZoneResolver.java:25`
- `src/main/java/attune/medication/application/MedicationService.java:340`
- `src/main/java/attune/medication/application/MedicationService.java:393`
- `src/main/java/attune/medication/domain/model/UserMedicationLog.java:32`
- `src/main/java/attune/medication/domain/repository/UserMedicationLogRepository.java`
- `src/main/java/attune/medication/application/MedicationLogSaver.java`
- `src/main/java/attune/medication/application/dto/request/QuickLogRequest.java:16`
- `src/main/java/attune/medication/application/dto/response/MedicationPeriodLogResponse.java:14`
- `src/main/java/attune/alarm/application/MedicationAlarmScheduler.java:57`
- `src/main/java/attune/alarm/application/NotificationTxOperations.java:41`
- `src/main/java/attune/alarm/domain/model/NotificationHistory.java`
- `src/main/java/attune/alarm/domain/repository/NotificationHistoryRepository.java`

## 사전 결정 게이트

아래 항목은 스키마 또는 공개 API 계약을 변경하므로 구현 전에 사람의 승인을 받는다.

1. `user_medication_logs`에 절대 시각, 현지 복약일, 기록 timezone 컬럼을 추가하는 최종 물리 스키마
2. 기존 `taken_at` 데이터를 KST로 해석하여 변환하는 마이그레이션 정책
3. `notification_histories.alarm_scheduled_at` 교체 또는 신규 절대시각 컬럼 병행 여부
4. 복약 기록 응답의 기존 `takenAt` 형식을 유지할 호환 기간과 제거 시점
5. timezone 변경 직후 같은 복약 스케줄의 최소 재알림 간격을 둘지 여부

## 작업 단계

1. 시간 의미와 현재 DB 왕복 동작 고정
   - MySQL `TIMESTAMP`/`DATETIME(6)`과 `Instant`/`LocalDateTime` 조합을 Testcontainers 통합 테스트로 확인한다.
   - JVM timezone을 `Asia/Seoul`과 UTC로 각각 실행하여 값의 저장·조회 의미를 기록한다.
   - 결과를 바탕으로 신규 절대시각 컬럼의 물리 타입을 확정한다.

2. 공용 시간 계산 기반 도입
   - UTC `Clock` Bean을 추가한다.
   - 사용자 timezone, 복약 현지 날짜, 현지 스케줄의 예정 `Instant`를 계산하는 컴포넌트를 추가한다.
   - 서비스와 스케줄러의 직접 `Instant.now()` 및 고정 `SERVER_ZONE` 사용을 공용 계산기로 교체한다.

3. 스키마 확장 및 기존 데이터 백필
   - 승인된 수동 SQL을 `docs/sql/`에 추가한다.
   - 우선 nullable 신규 컬럼을 추가하고 기존 KST 데이터를 백필한다.
   - 백필 결과와 중복 데이터를 검증한 후 NOT NULL 및 유일 제약을 적용한다.
   - `active_dose_date`가 명시적인 현지 복약일에서 파생되도록 변경한다.
   - 운영 `ddl-auto=validate`보다 SQL이 먼저 적용되는 배포 순서를 문서화한다.

4. 복약 기록 쓰기 경로 전환
   - `MedicationService`가 사용자 timezone을 한 번 해석하여 실제 복약 시각, 현지 복약일, 적용 timezone을 함께 생성하도록 변경한다.
   - `MedicationLogSaver`와 엔티티 생성·수정 메서드가 세 값을 원자적으로 저장하도록 변경한다.
   - 미래 시각 검증은 `Instant`끼리 비교하고, 일별 중복 검증은 `doseDate`를 사용한다.
   - 요청에 `takenAt`이 없으면 주입된 `Clock`의 현재 `Instant`를 사용한다.

5. 복약 기록 읽기 및 API 호환 전환
   - 기간 조회를 `doseDate` 기준으로 변경한다.
   - 실제 시각, 현지 복약일, 기록 timezone을 응답에 추가한다.
   - 기존 필드를 즉시 제거하지 않고 호환 필드를 병행한 뒤 프론트 전환 상태를 확인한다.
   - OpenAPI/API 가이드에 절대 시각, 날짜 전용 값, 벽시계 시간의 차이를 명시한다.

6. 복약 알림 예정 시각 및 중복 방지 전환
   - timezone별 후보 조회 구조는 유지한다.
   - 후보별 현지 날짜와 `doseTime`을 `ZoneId`로 결합하여 예정 `Instant`를 계산한다.
   - 알림 이력의 중복 키가 예정 절대시각을 사용하도록 저장·조회 경로를 변경한다.
   - 이미 복용 완료된 현지 복약일과 이미 처리된 예정 시각을 건너뛴다.
   - timezone 변경 중 조회된 설정과 발송 직전 설정이 달라질 수 있는 경쟁 조건을 테스트하고, 한 트랜잭션/스냅샷 기준을 정한다.

7. DST 및 timezone 변경 정책 구현
   - DST gap은 Java `ZoneRules`가 조정한 다음 유효 시각에 한 번만 발송한다.
   - DST overlap은 앞선 offset을 선택하고 절대시각 중복 키로 한 번만 발송한다.
   - 서울→뉴욕, 뉴욕→서울 변경 직후의 복구 윈도우와 재발송 여부를 검증한다.
   - 최소 재알림 간격을 도입하기로 승인된 경우 상수 또는 설정값과 테스트를 추가한다.

8. 점진적 배포
   - 1차 배포: 신규 컬럼 추가와 기존 데이터 백필, 애플리케이션은 구·신 필드를 병행 기록한다.
   - 2차 배포: 읽기·중복 판정을 신규 필드로 전환하고 관측 지표를 확인한다.
   - 3차 배포: 프론트 전환 확인 후 구 응답 및 구 컬럼 제거 여부를 별도 작업으로 결정한다.
   - 배포 단계별 실패 시 신규 경로를 끌 수 있도록 임시 feature flag 필요 여부를 결정한다.

9. 문서 및 전체 검증
   - DB 스키마, 글로벌 알림 명세, API 가이드와 테스트 문서를 실제 구현에 맞게 갱신한다.
   - 표준 전체 검증을 실행하고 아키텍처 규칙과 문서 점검을 통과한다.

## 검증 방법

### 단위 테스트

- 동일 `Instant`가 `Asia/Seoul`, `America/New_York`에서 서로 다른 현지 날짜가 되는지 검증
- `doseTime=09:00`이 timezone 변경 후에도 새 지역의 09:00으로 계산되는지 검증
- 설정 누락·잘못된 timezone의 기본값 폴백 검증
- DST gap의 다음 유효 시각 계산 검증
- DST overlap의 앞선 offset 선택 및 1회 발송 검증
- `Clock.fixed(...)`를 사용한 미래 시각 및 자정 경계 검증

### 통합 테스트

- 사용자 timezone PATCH 후 복약 기록 생성과 DB의 `taken_at`, `dose_date`, `dose_timezone` 확인
- 뉴욕 자정 전후 기록이 뉴욕 날짜로 귀속되는지 확인
- 활성 로그 취소·재등록 시 현지 복약일 유일 제약 확인
- 서버 재시작 복구 윈도우에서 동일 예정 `Instant`가 중복 발송되지 않는지 확인
- JVM timezone을 UTC로 실행해도 복약 계산 및 DB 왕복 결과가 동일한지 확인
- 기존 KST 샘플 데이터 백필 결과 확인

### 표준 명령

```bash
./gradlew test --tests "attune.user.application.UserZoneResolverTest"
./gradlew test --tests "attune.medication.*"
./gradlew test --tests "attune.alarm.application.MedicationAlarmSchedulerTest"
bash scripts/agent/check-docs
bash scripts/agent/verify
```

## 관측 및 운영 확인

- 잘못된 timezone으로 건너뛴 사용자 수
- timezone별 복약 알림 후보·발송·중복 스킵 수
- 기존 필드와 신규 필드의 복약일 불일치 수
- 유일 제약 충돌 수
- DST 전환일의 중복 발송 및 누락 여부
- 배포 후 KST 사용자 알림 발송량이 기존 기준에서 급변하지 않는지 확인

로그에는 사용자 식별정보와 복약 상세를 직접 남기지 않고 schedule ID, timezone, 처리 결과처럼 필요한 최소 정보만 남긴다.

## 위험 요소

- 기존 `taken_at`의 의미를 잘못 해석하면 과거 기록이 9시간 이동할 수 있다.
- 공개 API의 `takenAt` 형식을 즉시 변경하면 기존 프론트가 날짜를 잘못 표시할 수 있다.
- 복약일 유일 제약 전환 중 기존 중복 데이터 때문에 DDL이 실패할 수 있다.
- timezone 변경 직후 복구 윈도우가 이전 timezone과 새 timezone 후보를 모두 포함하면 중복 발송될 수 있다.
- DST gap/overlap을 단순 `LocalDateTime` 비교로 처리하면 누락 또는 이중 발송될 수 있다.
- JVM KST, JDBC, DB session timezone이 일치하지 않으면 절대시각 저장값이 환경별로 달라질 수 있다.
- 여러 애플리케이션 버전이 동시에 실행되는 배포 중 구·신 스키마 접근이 충돌할 수 있다.

## 롤백 방법

1. 신규 필드는 기존 코드와 공존하도록 additive migration으로 먼저 배포한다.
2. 읽기·중복 판정 전환에 문제가 생기면 애플리케이션을 구 필드 기준으로 되돌린다.
3. 신규 컬럼 기록은 유지하여 원인 분석과 재전환에 사용한다.
4. 데이터가 안정화되기 전에는 기존 `taken_at` 및 기존 알림 예정 시각 컬럼을 삭제하지 않는다.
5. 신규 유일 제약이 장애를 일으키면 쓰기 경로를 구 기준으로 되돌린 뒤 충돌 데이터를 조사한다.
6. 컬럼 삭제와 파괴적 롤백은 별도 승인 및 백업 확인 없이는 수행하지 않는다.

## 의사결정 로그

- 2026-07-16 복약 `doseTime`은 사용자의 최신 timezone을 따르는 현지 벽시계 시각으로 확정했다.
- 2026-07-16 여행 모드, 홈 timezone 및 약별 timezone 정책은 현재 요구에 필요하지 않아 제외했다.
- 2026-07-16 과거 기록의 의미 보존을 위해 실제 순간, 현지 복약일, 당시 timezone을 분리하는 방향을 채택했다.
- 2026-07-16 기존 알림 후보 조회는 유지하고 중복 식별을 절대 예정 시각으로 강화하기로 했다.
- 2026-07-16 스키마와 공개 API의 최종 변경은 사전 결정 게이트 승인 후 구현하기로 했다.

## 완료 조건

- [ ] 사용자가 timezone을 변경하면 기존 `doseTime`이 새 지역의 동일 현지 시각으로 적용된다.
- [ ] 복약 기록의 실제 순간이 절대 시각으로 보존된다.
- [ ] 복약 기록의 날짜가 기록 당시 사용자 timezone 기준으로 저장되고 이후 timezone 변경에도 유지된다.
- [ ] 알림 중복 방지가 예정 절대시각을 기준으로 동작한다.
- [ ] timezone 변경 직후 동일 알림이 의도치 않게 중복 발송되지 않는다.
- [ ] DST gap과 overlap 테스트가 통과한다.
- [ ] JVM timezone을 UTC로 바꾼 테스트에서도 복약 결과가 동일하다.
- [ ] 기존 KST 기록의 마이그레이션 결과가 검증된다.
- [ ] API 호환 전환과 프론트 연동 계약이 문서화된다.
- [ ] 전체 빌드, 테스트, 아키텍처 및 문서 검증이 통과한다.

## 작업 후 문서 업데이트 목록

- [ ] `docs/db_schema.md`
- [ ] `docs/database.md`
- [ ] `docs/global-time-alarm-spec.md`
- [ ] `docs/api-guide.md`
- [ ] `docs/engineering/testing-strategy.md`
- [ ] `docs/generated/data-schema.md` — 생성 스크립트로 갱신하고 직접 수정하지 않는다.
- [ ] 관련 SQL 및 배포 순서 문서
