# 시간 처리 및 글로벌 알림 시스템 개발 명세서

## 1. 목표

Attune은 현재 국내 사용자가 대부분이지만, 해외 사용자 유입과 국내 사용자의 해외 체류를 고려해 시간 처리 기준을 명확히 분리한다.

핵심 목표는 다음과 같다.

- 화면에는 사용자의 기기 현지 시간 기준으로 자연스럽게 표시한다.
- 복약 알림은 사용자가 현재 있는 지역의 벽시계 시간 기준으로 발송한다.
- 서버와 DB 인프라의 시스템 timezone에 의존하지 않는다.
- 현재 국내 중심 운영 현실에 맞춰, 실제 저장된 사용자 timezone만 대상으로 계산한다.

## 2. 핵심 원칙

시간 값은 한 종류가 아니다. 아래 세 종류를 구분해서 저장하고 API로 전달한다.

| 구분 | 의미 | 예시 | 저장/API 원칙 |
|------|------|------|---------------|
| 실제 발생 시각 | 어떤 일이 실제로 일어난 한 순간 | 복약 완료 버튼을 누른 시각, 알림 발송 시각 | `Instant` 또는 offset 포함 datetime |
| 벽시계 시간 | 사용자가 의도한 하루 중 시각 | 매일 아침 8시 복약 | `LocalTime`, 예: `08:00:00` |
| 사용자 기준 날짜 | 사용자 timezone에서의 날짜 | 뉴욕 기준 7월 1일 | `LocalDate` + 사용자 `ZoneId`로 계산 |

절대 금지한다.

- 복약 목표 시각에 `+09:00` 같은 offset을 붙이지 않는다.
- 모든 datetime을 무조건 KST로 변환해 내려주지 않는다.
- 서버 JVM이나 DB timezone 설정에 비즈니스 로직을 의존시키지 않는다.

## 3. 비즈니스 규칙

### 3.1 복약 체크 매칭

복약 여부는 시간 문자열 비교가 아니라 고유 ID인 `scheduleId` 기준으로 판단한다.

- 복약 스케줄: `user_medication_schedules.id`
- 복약 로그: `user_medication_schedule_id`

### 3.2 화면 시간 표시

실제 발생 시각은 서버가 timezone 정보가 포함된 ISO-8601 문자열로 내려준다.

허용 예:

```text
2026-07-01T14:00:00+09:00
2026-07-01T05:00:00Z
```

프론트엔드는 해당 값을 그대로 `new Date(serverTime)`으로 파싱한다.

복약 목표 시각은 벽시계 시간이므로 timezone 변환 대상이 아니다.

예:

```text
08:00:00
```

### 3.3 복약 알림

복약 알림은 사용자의 최신 timezone 기준 현지 벽시계 시간에 맞춰 발송한다.

예:

- 사용자의 복약 시간이 `08:00:00`
- 사용자 timezone이 `Asia/Seoul`이면 한국 오전 8시에 발송
- 사용자 timezone이 `America/New_York`이면 뉴욕 오전 8시에 발송

## 4. 프론트엔드 요구사항

### 4.1 화면 시간 표시 로직 정리

기존의 수동 KST 계산 함수와 화면별 파편화된 시간 파싱 로직을 제거한다.

제거 대상 예:

- `toKstMinutes`
- 직접 `getHours()`를 조합해 KST 보정하는 로직
- 화면마다 다른 timezone 보정 로직

변경 후 원칙:

```javascript
const date = new Date(serverTime);
```

서버가 offset 또는 `Z`를 포함한 datetime을 내려주면 브라우저가 기기 timezone에 맞게 자동 표시한다.

### 4.2 복약 목표 시각 표시

복약 목표 시각은 `08:00:00` 같은 벽시계 시간이다.

프론트엔드는 이 값을 `Date`로 변환하지 않는다. 필요한 경우 문자열 또는 `hour/minute` 파싱으로 표시한다.

예:

```text
08:00:00 -> 오전 8:00
```

### 4.3 사용자 timezone 동기화

앱 실행 또는 로그인 시 브라우저 timezone을 확인한다.

```javascript
const userTimezone = Intl.DateTimeFormat().resolvedOptions().timeZone;
```

권장 동작:

- 현재 서버에 저장된 timezone과 다를 때만 업데이트 API를 호출한다.
- 값이 비어 있거나 브라우저가 timezone을 제공하지 못하면 서버 기본값을 사용한다.

전송 값 예:

```text
Asia/Seoul
America/New_York
Europe/London
```

서버 반영은 기존 사용자 설정 API를 확장해서 처리한다.

```http
PATCH /v1/users/settings
Authorization: Bearer ...
Content-Type: application/json
```

요청 예:

```json
{
  "timezone": "America/New_York"
}
```

알림 설정과 함께 보내도 된다.

```json
{
  "medicationNotification": true,
  "todoNotification": true,
  "timezone": "Asia/Seoul"
}
```

## 5. 백엔드 요구사항

### 5.1 API 시간 포맷

API 응답 시간은 아래 기준을 따른다.

| 필드 유형 | 예시 필드 | 응답 포맷 |
|-----------|-----------|-----------|
| 실제 발생 시각 | `takenAt`, `recordedAt`, `createdAt`, `updatedAt`, `sentAt` | offset 포함 datetime 또는 UTC `Z` |
| 복약 목표 시각 | `doseTime`, `targetTime` | `HH:mm:ss` |
| 날짜 | `startedAt`, `endAt`, `journalDate` | `yyyy-MM-dd` |

실제 발생 시각 예:

```json
{
  "takenAt": "2026-07-01T14:00:00+09:00"
}
```

또는:

```json
{
  "takenAt": "2026-07-01T05:00:00Z"
}
```

복약 목표 시각 예:

```json
{
  "doseTime": "08:00:00"
}
```

### 5.2 사용자 timezone 저장

사용자의 최신 timezone을 저장한다.

이번 범위에서는 별도 timezone 테이블을 만들지 않는다.

저장 위치는 `user_settings.timezone`으로 확정한다.

이유:

- 현재 필요한 값은 사용자별 최신 timezone 1개뿐이다.
- timezone은 복약 알림, 리포트 알림 등 사용자 알림 설정과 직접 연결된다.
- 변경 이력, 기기별 timezone, 여행 모드 같은 확장 정책은 이번 범위가 아니다.
- 별도 테이블을 만들면 조인과 동기화 규칙만 늘어나고 현재 문제 해결에는 이득이 작다.

컬럼:

```sql
ALTER TABLE user_settings
  ADD COLUMN timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Seoul';
```

검증:

- 서버는 `ZoneId.of(timezone)`으로 유효한 IANA timezone인지 검증한다.
- 유효하지 않은 timezone은 `400 Bad Request`로 거부한다.

인덱스:

```sql
CREATE INDEX idx_user_settings_timezone
  ON user_settings (timezone);
```

### 5.3 사용자 설정 API 확장

기존 사용자 설정 API를 확장한다. timezone만 업데이트하기 위한 별도 API는 만들지 않는다.

대상 API:

```http
GET /v1/users/settings
PATCH /v1/users/settings
```

`PATCH /v1/users/settings` 요청 필드에 `timezone`을 추가한다.

요청:

```json
{
  "timezone": "America/New_York"
}
```

기존 설정 필드와 함께 전달할 수 있다.

```json
{
  "medicationNotification": true,
  "reportNotification": true,
  "marketingNotification": false,
  "communityNotification": true,
  "todoNotification": true,
  "takeMedicationOnHoliday": false,
  "theme": "SYSTEM",
  "timezone": "Asia/Seoul"
}
```

응답에는 현재 저장된 timezone을 포함한다.

```json
{
  "medicationNotification": true,
  "reportNotification": true,
  "marketingNotification": false,
  "communityNotification": true,
  "todoNotification": true,
  "takeMedicationOnHoliday": false,
  "theme": "SYSTEM",
  "timezone": "Asia/Seoul"
}
```

검증 규칙:

- `timezone`이 요청에 없거나 `null`이면 기존 timezone을 변경하지 않는다.
- `timezone`이 빈 문자열이면 `400 Bad Request`를 반환한다.
- `timezone`이 있으면 `ZoneId.of(timezone)`으로 검증한다.
- 유효하지 않은 IANA timezone이면 `400 Bad Request`를 반환한다.
- 신규 사용자 기본 timezone은 `Asia/Seoul`이다.

### 5.4 복약 목표 시각 저장

복약 목표 시각은 순수 벽시계 시간으로 저장한다.

현재 구조인 `user_medication_schedules.dose_time TIME`을 유지한다.

예:

```text
08:00:00
```

추가 권장 인덱스:

```sql
CREATE INDEX idx_user_medication_schedules_dose_time_active
  ON user_medication_schedules (dose_time, is_active);
```

## 6. 복약 알림 스케줄러

### 6.1 실행 기준

스케줄러는 1분마다 실행한다.

실행 트리거 자체는 서버 timezone에 의존하지 않도록 한다. 내부 계산은 `Instant.now(clock)` 기준으로 수행한다.

### 6.2 처리 흐름

1. 활성 사용자 설정에서 timezone 목록을 중복 제거해 조회한다.

```sql
SELECT DISTINCT timezone
FROM user_settings
WHERE timezone IS NOT NULL;
```

2. Java 애플리케이션에서 현재 `Instant`를 각 timezone의 현지 시간으로 변환한다.

예:

```text
Instant: 2026-07-01T00:00:00Z
Asia/Seoul -> 2026-07-01 09:00
America/New_York -> 2026-06-30 20:00
```

3. timezone별 현재 현지 시각의 `LocalTime`을 구한다.

4. 해당 timezone 사용자 중 복약 목표 시각이 현재 현지 시각과 일치하는 스케줄만 조회한다.

개념 쿼리:

```sql
SELECT ums.*
FROM user_medication_schedules ums
JOIN user_medications um ON ums.user_medication_id = um.id
JOIN user_settings us ON um.user_id = us.user_id
JOIN users u ON um.user_id = u.id
WHERE (
       (us.timezone = 'Asia/Seoul' AND ums.dose_time = '09:00:00')
    OR (us.timezone = 'America/New_York' AND ums.dose_time = '20:00:00')
)
AND ums.is_active = true
AND um.is_active = true
AND um.alarm_active = true
AND u.user_status = 'ACTIVE';
```

5. 각 대상에게 Web Push를 발송한다.

### 6.3 중복 발송 방지 기준

중복 발송 방지 키는 기존 `notification_history`의 unique 제약을 유지한다.

```text
user_id + alarm_type + reference_id + alarm_scheduled_at
```

복약 알림의 `alarm_scheduled_at`은 사용자 timezone 기준의 `LocalDate + doseTime`으로 만든다.

예:

```text
사용자 timezone: America/New_York
현지 날짜: 2026-06-30
doseTime: 20:00:00
alarmScheduledAt: 2026-06-30T20:00:00
```

이 값은 “사용자에게 의도된 현지 알림 슬롯”을 나타내는 중복 방지 값이다.

### 6.4 복구 윈도우

스케줄러가 몇 분 지연되어도 알림 누락이 없도록 복구 윈도우를 유지한다.

단, 복구 윈도우 계산도 각 timezone의 현지 `LocalTime` 기준으로 수행한다.

예:

- 현재 현지 시각: `09:05`
- 복구 윈도우: 10분
- 조회 대상: `08:55`부터 `09:05`까지

자정 경계를 넘는 경우를 처리한다.

예:

- 현재 현지 시각: `00:03`
- 복구 윈도우: 10분
- 조회 대상: `23:53`부터 `00:03`까지

## 7. DST 정책

IANA timezone은 DST가 있을 수 있으므로 정책을 명시한다.

### 7.1 존재하지 않는 시간

DST 시작으로 특정 현지 시간이 존재하지 않는 경우, 해당 알림은 다음 유효한 현지 시각에 1회 발송한다.

예:

```text
02:30이 존재하지 않는 날 -> 03:00 또는 해당 timezone의 다음 유효 시각
```

### 7.2 두 번 오는 시간

DST 종료로 같은 현지 시간이 두 번 오는 경우, 같은 `user_id + scheduleId + localDate + doseTime` 기준으로 1회만 발송한다.

중복 여부는 `notification_history`의 unique 제약으로 보장한다.

## 8. 인프라 시간대

> **구현 현황(2026-07):** 인프라 UTC 전환은 **보류**하고 서버/JVM/DB를 `Asia/Seoul`로 고정 운영한다.
> 현재 국내 전용 단계에서는 KST 고정이 단순하고 naive `LocalDateTime` 저장값과 일관되기 때문이다.
> 단 사용자별 timezone(`user_settings.timezone`)과 복약 알림 per-user 계산은 도입해 두어(=`Instant` 기반, 서버 TZ 무관)
> 향후 글로벌 전환 시 아래 UTC 목표로 이동하는 비용을 낮췄다. 아래는 그 **최종 지향 설계**다.

서버, JVM, DB 시스템, DB 연결 timezone은 UTC를 유지한다.

이유:

- 클라우드 로그, Sentry, CloudWatch와 정렬하기 쉽다.
- 서버 지역 변경이나 컨테이너 설정 변경이 비즈니스 로직에 영향을 주지 않는다.
- 사용자 timezone 계산은 Java의 `Instant`와 `ZoneId`로 처리한다.

구현 설정:

- JVM: `-Duser.timezone=UTC`
- Spring Boot 시작 시 `TimeZone.setDefault(TimeZone.getTimeZone("UTC"))`
- Hibernate: `hibernate.jdbc.time_zone=UTC`
- MySQL JDBC URL: `serverTimezone=UTC`

개발 환경에서 디버깅이 필요하면 모니터링 도구 UI에서 local time 표시를 사용한다.

## 9. 테스트 요구사항

필수 테스트:

- `Asia/Seoul` 사용자가 `08:00` 복약 알림을 한국 오전 8시에 받는다.
- `America/New_York` 사용자가 `08:00` 복약 알림을 뉴욕 오전 8시에 받는다.
- 같은 `Instant`에서 timezone별 현지 시간이 다르게 계산된다.
- 자정 경계를 넘는 복구 윈도우가 누락 없이 조회된다.
- DST로 존재하지 않는 시간이 다음 유효 시각에 1회 처리된다.
- DST로 두 번 오는 시간이 1회만 처리된다.
- 유효하지 않은 timezone 업데이트 요청은 거부된다.
- 서버 JVM timezone을 UTC로 바꿔도 알림 계산 결과가 변하지 않는다.

## 10. 이번 범위에서 제외

아래 항목은 별도 정책 결정 후 진행한다.

- 일정/캘린더 알림의 원본 timezone 보존 정책
- 사용자가 여행 중일 때 “현재 현지 시간 따라가기”와 “원래 국가 시간 유지하기”를 선택하는 기능
- 사용자에게 timezone 변경 확인 UI를 띄우는 정책
- 기존 저장 datetime 전체를 UTC 기반 타입으로 마이그레이션하는 대규모 작업
