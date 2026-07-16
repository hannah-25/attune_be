# 실행 계획: 여행 중 현지시간 기준 복약 처리 일관화

- 상태: completed
- 작성자 / 날짜: Codex / 2026-07-16 (2026-07-16 범위 축소 후 구현 완료)
- 관련 이슈/PR: 미정

## 목표

사용자가 출장이나 여행으로 timezone을 변경했을 때 복약 목표 시각(`doseTime`)을 체류지의 현지 벽시계 시각으로
해석하고, 복약 기록이 체류지 현지 날짜에 귀속되게 한다.

예: `doseTime=09:00`인 사용자가 서울에서 뉴욕으로 이동해 timezone을 `America/New_York`으로 갱신하면,
알림은 뉴욕 오전 9시에 발송되고 복약 기록도 뉴욕 현지 날짜에 귀속된다.

## 배경

`user_settings.timezone`에 사용자의 최신 IANA timezone ID를 저장한다.

- **복약 알림**: `MedicationAlarmScheduler`가 이미 사용자별 timezone으로 현지 시각을 계산해 후보를 조회한다.
  즉 알림은 **이미 현지시간을 따르고 있었다.** 이 부분은 변경이 필요하지 않았다.
- **복약 기록**: 클라이언트가 보낸 `Instant`를 고정 `Asia/Seoul`의 `LocalDateTime`으로 변환해 저장했다.
  복약일(`active_dose_date`)이 `DATE(taken_at)`에서 파생되므로 **해외 사용자의 복용일이 KST 날짜로 계산**됐다.

따라서 실제 결함은 "복약 기록의 복용일 귀속" 한 곳이었다.

## 범위 축소 결정 (2026-07-16)

초안은 복약 기록을 절대시각(`Instant`/`TIMESTAMP`)으로 저장하고, 스키마 확장·기존 데이터 백필·구신 필드
이중 기록·3단계 점진 배포를 포함하는 대규모 마이그레이션이었다. **이 접근은 폐기했다.**

근거:

- 사용 프로파일이 국내 사용자 약 99%이고, 해외 체류는 2~3일 출장 수준으로 드물고 짧다.
- 알림은 이미 현지시간으로 동작하므로, 남은 결함은 복용일 귀속 하나뿐이다.
- 코어 테이블(`user_medication_logs`) 스키마 변경 + 백필 + 3단계 배포의 위험과 비용이 이 엣지케이스의
  가치를 크게 초과한다.
- 사전 조사에서 MySQL `TIMESTAMP`의 왕복 의미가 JDBC 커넥션/JVM 기본 timezone에 의존함을 확인했다.
  절대시각 도입은 환경별 편차 위험을 새로 들여오는 반면, 얻는 것은 "여행자 기록의 절대 순간 복원"뿐이며
  이 제품에는 그 수요가 없다.

채택안: **복약 기록의 `takenAt`을 서버 고정 KST가 아니라 기록 시점 사용자 timezone의 현지 벽시계로
저장하고, 그때 쓴 timezone을 `dose_timezone`에 함께 남긴다.**

`dose_timezone`은 처음에 제외했다가 코드 리뷰에서 되살렸다. 이것 없이는 오프라인 재전송 중 timezone이
바뀔 때 중복 로그가 생기며, 이는 타깃 시나리오(2~3일 출장) 안에서 발생한다. 아래 의사결정 로그 참고.
컬럼 1개 additive 추가일 뿐 백필 이후 이중 기록·3단계 배포는 필요 없다.

## 확정 정책

- 복약 목표 시각 `doseTime`은 timezone이 없는 현지 벽시계 시각이다.
- 복약 알림은 항상 사용자의 최신 `user_settings.timezone`을 따른다. (기존 동작 유지)
- 복약 기록 `takenAt`은 기록 시점 사용자 timezone의 현지 벽시계로 저장한다.
- 기록에 쓴 timezone은 `dose_timezone`에 남긴다. `takenAt`과 합치면 실제 복용 순간이 복원된다.
- 복약일은 `takenAt`의 날짜이며, 결과적으로 기록 당시 현지 날짜가 된다.
- 별도의 절대시각(`Instant`/`TIMESTAMP`) 컬럼은 도입하지 않는다. `taken_at` + `dose_timezone`이
  같은 정보를 담는다.
- 오프라인 재전송은 복용 순간이 같으면 timezone이 바뀌었더라도 같은 복용으로 식별한다.
  이때 원래 복용일·timezone 귀속을 보존하고 상태만 갱신한다.
- `homeTimezone`, 여행 모드, 약별 timezone 정책은 도입하지 않는다.
- 서버가 시차에 따른 의학적 복용 간격을 임의로 조정하지 않는다.

## 변경 내용

- `MedicationService.quickLog`
  - `UserZoneResolver.resolve(userId)`로 사용자 timezone을 한 번 해석한다.
  - 현재 시각을 `LocalDateTime.now(userZone)`으로 계산한다.
  - `resolveTakenAt`이 클라이언트 `Instant`를 `SERVER_ZONE`이 아닌 사용자 timezone의 벽시계로 변환한다.
  - `findExistingLogForDose`가 같은 복용일 활성 로그를 먼저 찾고, 없고 클라이언트가 절대 시각을 보냈다면
    인접 복용일(±2일) 활성 로그 중 기록 당시 timezone으로 복원한 복용 순간이 일치하는 것을 같은 복용으로 본다.
  - 미사용이 된 `SERVER_ZONE` 상수를 제거했다.
- `UserMedicationLog`
  - `doseTimezone` 필드 추가. `update`가 timezone도 함께 갱신하고, 재전송용 `updateStatus`를 추가했다.
- `MedicationPeriodLogResponse`
  - 고정 `+09:00` 부착을 제거하고 기록 당시 timezone으로 offset을 계산한다.
- `docs/sql/20260716_medication_log_dose_timezone.sql`
  - `dose_timezone` 컬럼 추가 + 기존 행 `Asia/Seoul` 백필. 운영에 수동 선적용한다.

`now`와 `takenAt`은 같은 timezone 기준이어야 한다. 서로 다르면 클라이언트 시계 오차(skew)·큐 만료 검증이
시차만큼 어긋나 정상 요청을 400으로 거절한다.

`DOSE_DATE_SEARCH_RADIUS_DAYS = 2`인 이유: IANA offset은 UTC-12..UTC+14라 같은 순간의 현지 시각이 최대
26시간 벌어지고, 현지 날짜는 최대 2일까지 차이날 수 있다. 2일이면 모든 timezone 조합을 덮는다.

변경하지 않은 것:

- `MedicationAlarmScheduler` — 이미 사용자 timezone을 따름
- `UserMedication.createdAt/updatedAt` — 복용일 귀속과 무관한 감사 메타데이터
- 기간 조회 헬퍼(`toStartOfDay`/`toExclusiveEndOfDay`) — timezone 무관. `takenAt`과 조회 파라미터가
  모두 사용자 현지 날짜 기준이 되어 자동으로 일관된다.

## 알려진 한계

- **`MedicationLogResponse.takenAt`**: `GET /v1/user-medications/{id}/logs`의 `takenAt`은 offset 없는
  naive datetime이라 스펙 5.1의 "offset 포함" 규정과 어긋난다(선행 문제). 클라이언트는 이 값을 현지
  벽시계로만 읽어야 하며 절대 순간으로 환산하면 안 된다. 계약 변경이라 이번 범위에서 제외했다.
- **혼재된 의미**: `taken_at` 컬럼은 행마다 "그 시점 사용자 timezone의 벽시계"다. 절대 순간은
  `dose_timezone`과 합쳐야 복원된다. `taken_at`만 단독으로 비교·정렬하면 timezone이 다른 행 사이에서
  절대 순서가 어긋날 수 있다.
- **DST overlap 중 재전송**: 현지 시각이 두 번 오는 1시간 구간에 기록된 복용을 재전송하면,
  `taken_at.atZone(dose_timezone)`이 앞선 offset을 선택해 복원 순간이 원본과 어긋날 수 있다. 이 경우
  재전송이 같은 복용으로 식별되지 않아 중복 로그가 생긴다. 연 1회 1시간 구간에 한정돼 수용한다.
- **알림 중복 식별**: `notification_history.alarm_scheduled_at`은 timezone 없는 현지 `LocalDateTime`이다.
  timezone 변경 직후나 DST 전환 시 중복/누락 가능성이 이론적으로 남아 있다. 발생 빈도와 피해가 낮아
  이번 범위에서 제외했다.

## 재검토 트리거

아래 중 하나라도 성립하면 절대시각 도입을 다시 검토한다.

- 해외 사용자 비중이 유의미해지거나 장기 체류(수 주 이상) 사용 사례가 생긴다.
- 복약 기록의 절대 순간이 필요한 요구(의학적 감사, 복용 간격 분석, 기관 연동)가 생긴다.
- 복약 기록을 timezone이 다른 외부 시스템과 대사(reconcile)해야 한다.

그 경우 경로: `dose_timezone` 컬럼(nullable, additive)을 먼저 추가해 기록 당시 timezone을 남기고,
기존 행은 `Asia/Seoul`로 간주한다. 그것으로 부족할 때만 절대시각 컬럼을 검토한다.

## 검증 방법

### 통합 테스트 (HTTP→DB 전 구간)

`MedicationIntegrationTest.doseTakenAbroadIsAttributedToLocalDateNotKst`

- 사용자가 timezone을 `America/New_York`으로 PATCH한다.
- 뉴욕 어제 20:00(KST로는 하루 뒤)에 복용을 기록한다.
- 뉴욕 현지 날짜에 활성 로그 1건이 귀속되고, KST 날짜 조회는 비어 있음을 확인한다.

수정 전 코드에서 실패하고 수정 후 통과함을 확인했다(회귀 방지 성립).

`MedicationIntegrationTest.replayAfterTimezoneChangeDoesNotDuplicateLog`

- 뉴욕에서 복용을 기록한 뒤 timezone을 `Asia/Seoul`로 되돌리고 같은 절대 시각을 재전송한다.
- 원래 복용일(뉴욕 날짜)에 활성 로그가 1건만 남고, 새 timezone으로 계산된 날짜에는 생기지 않음을 확인한다.

`DOSE_DATE_SEARCH_RADIUS_DAYS`를 0으로 두어 교차 timezone 매칭을 무력화하면 실패하고, 정상값에서는
통과함을 확인했다(회귀 방지 성립).

### 표준 명령

```bash
./gradlew test --tests "attune.medication.*"
./gradlew test --tests "attune.user.application.UserZoneResolverTest"
bash scripts/agent/check-docs
bash scripts/agent/verify
```

## 의사결정 로그

- 2026-07-16 복약 `doseTime`은 사용자의 최신 timezone을 따르는 현지 벽시계 시각으로 확정했다.
- 2026-07-16 여행 모드, 홈 timezone 및 약별 timezone 정책은 현재 요구에 필요하지 않아 제외했다.
- 2026-07-16 사전 조사로 알림 경로가 이미 사용자 timezone을 따름을 확인했다. 실제 결함은 복약 기록의
  복용일 귀속 한 곳으로 좁혀졌다.
- 2026-07-16 MySQL `TIMESTAMP`/`DATETIME` 왕복 실측에서 `TIMESTAMP`는 `Instant`를 정확히 왕복하지만
  변환이 JDBC/JVM 기본 timezone에 의존함을 확인했다. `DATETIME`은 timezone 정보를 잃어 절대시각 저장에
  부적합하다.
- 2026-07-16 **절대시각 저장·스키마 마이그레이션·3단계 배포 계획을 폐기했다.** 국내 99% / 단기 출장이라는
  사용 프로파일 대비 비용과 위험이 과도하다.
- 2026-07-16 채택: 복약 기록을 기록 시점 사용자 timezone의 현지 벽시계로 저장한다.
- 2026-07-16 코드 리뷰에서 위 변경이 **오프라인 재전송 멱등성을 깨뜨림**을 발견했다. 재전송 전에
  timezone이 바뀌면 같은 절대 시각이 다른 복용일로 계산돼 기존 로그를 놓치고 활성 로그가 2건 생긴다.
  유니크 제약은 복용일이 달라 막지 못한다. 재전송 윈도우가 7일이라 2~3일 출장이라는 타깃 시나리오
  **안에서** 발생하므로 수용할 수 없다고 판단했다.
- 2026-07-16 대응으로 `dose_timezone`(VARCHAR(64), nullable) 컬럼을 additive로 추가했다. `taken_at`과
  합쳐 원래 복용 순간을 복원해 재전송을 같은 복용으로 식별한다. 백필/이중 기록/3단계 배포는 불필요하다.
- 2026-07-16 교차 timezone으로 식별된 재전송은 `taken_at`을 덮지 않고 상태만 갱신한다. 덮으면 복용일이
  옮겨져 과거 기록의 귀속이 바뀌고 유니크 제약과 충돌한다.
- 2026-07-16 `dose_timezone`이 생겨 응답 offset을 실제 기록 timezone으로 계산할 수 있게 됐다.
  `MedicationPeriodLogResponse`의 고정 `+09:00` 부착을 제거했다(국내 기록은 결과 동일).

## 완료 조건

- [x] 사용자가 timezone을 변경하면 기존 `doseTime`이 새 지역의 동일 현지 시각으로 적용된다. (알림 경로 기존 동작)
- [x] 복약 기록의 날짜가 기록 당시 사용자 timezone 기준으로 귀속된다.
- [x] 국내 사용자의 기존 동작이 변하지 않는다(기본 timezone 폴백 `Asia/Seoul`).
- [x] 여행 시나리오 통합 테스트가 수정 전 실패·수정 후 통과로 회귀를 방지한다.
- [x] 오프라인 재전송이 timezone 변경 뒤에도 멱등이며, 회귀 테스트로 고정했다.
- [x] 절대시각 마이그레이션·백필·3단계 배포 없이 완료한다(additive 컬럼 1개).
- [x] 전체 빌드, 테스트, 아키텍처 및 문서 검증이 통과한다.

## 작업 후 문서 업데이트 목록

- [x] `docs/global-time-alarm-spec.md` — 복약 기록의 timezone 기준과 `dose_timezone` 명시
- [x] `docs/db_schema.md` — `dose_timezone` 컬럼 및 `taken_at` 의미 갱신
- [x] `docs/sql/20260716_medication_log_dose_timezone.sql` — 운영 선적용 SQL 추가
- [x] 본 실행 계획 — 범위 축소와 근거, 리뷰에서 발견한 멱등성 결함과 대응 기록

갱신하지 않은 문서와 이유:

- `docs/api-guide.md` — 응답 필드 구성이 그대로이고 국내 기록의 값도 동일하다.
- `docs/generated/data-schema.md` — 도메인·엔티티·`@Table` 인벤토리만 담고 컬럼을 추적하지 않으므로
  이번 변경과 무관하다. (재생성 시 이 변경과 상관없는 드리프트가 함께 발생하므로 별도 작업으로 다룬다.)
