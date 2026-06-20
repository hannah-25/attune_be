# Spring Clock Bean 리팩토링 계획

## 실행 결과

- 구현 상태: 완료
- `compileJava`: 성공
- `compileTestJava`: 성공
- Clock 리팩토링 영향 테스트 13개 클래스: 성공
- 전체 테스트 134개: 성공
- production 무인자 `LocalDateTime.now()`: 0건
- `ZoneId.of("Asia/Seoul")`: `AppClockConfig.java` 1건
- `adminAuditClock`: UTC 및 qualifier 구성 유지

## 목표

애플리케이션의 현재 시각 취득을 Spring `Clock` 빈으로 일원화한다.

- 서비스 계층은 `Clock`을 주입받아 `LocalDateTime.now(clock)`으로 현재 시각을 계산한다.
- 엔티티는 시스템 시계에 직접 접근하지 않고 mutation 메서드와 팩토리 메서드의 `LocalDateTime now` 인자로 시각을 전달받는다.
- 운영 애플리케이션 시계는 `Asia/Seoul`을 사용한다.
- 감사 로그 전용 `adminAuditClock`(UTC)은 변경하지 않는다.
- 테스트에서는 필요 시 `Clock.fixed(...)`를 주입해 시간 의존 동작을 결정적으로 검증한다.

## 1단계: 인프라

- [ ] `src/main/java/attune/common/config/AppClockConfig.java`
  - `@Configuration` 추가
  - `@Primary` `Clock appClock()` 빈 추가
  - `Clock.system(ZoneId.of("Asia/Seoul"))` 사용
- [ ] `src/main/java/attune/common/util/AppClock.java`가 존재하면 삭제
- [ ] `src/main/java/attune/admin/audit/config/AdminAuditConfig.java`의 `adminAuditClock`은 유지

## 2단계: 엔티티 시그니처 변경

엔티티 내부의 `LocalDateTime.now()` 및 inline `ZoneId` 사용을 제거하고 호출자가 `now`를 전달하도록 변경한다.

- [ ] `PasswordResetToken.isExpired(LocalDateTime now)`
- [ ] `EmailVerificationToken.isExpired(LocalDateTime now)`
- [ ] `NotificationSubscription.updateKeys(..., LocalDateTime now)`
- [ ] `NotificationSubscription.updateToken(..., LocalDateTime now)`
- [ ] `NotificationSubscription.disable(LocalDateTime now)`
- [ ] `CalendarConnection.google(..., LocalDateTime now)`
- [ ] `CalendarConnection.reconnect(..., LocalDateTime now)`
- [ ] `CalendarConnection.updateSelectedCalendarIds(..., LocalDateTime now)`
- [ ] `CalendarConnection.updateAccessToken(..., LocalDateTime now)`
- [ ] `CalendarConnection.deactivate(LocalDateTime now)`
- [ ] `CommunityBoard.update(..., LocalDateTime now)`
- [ ] `Comment.update(..., LocalDateTime now)`
- [ ] `Consultation.updateSchedule(..., LocalDateTime now)`
- [ ] `Consultation.updateResult(..., LocalDateTime now)`
- [ ] `Consultation.clearResult(LocalDateTime now)`
- [ ] `Consultation.delete(LocalDateTime now)`
- [ ] `UserMedication.update(..., LocalDateTime now)`
- [ ] `Notice.update(..., LocalDateTime now)`
- [ ] `UserJournalTagPreference.create(..., LocalDateTime now)`
- [ ] `UserJournalTagPreference.update(..., LocalDateTime now)`
- [ ] `JournalTag.userTag(..., LocalDateTime now)`
- [ ] `JournalTag.systemTag(..., LocalDateTime now)`
- [ ] `JournalTag.deactivate(LocalDateTime now)`
- [ ] `JournalTag.activate(LocalDateTime now)`
- [ ] `LegacyJournalTagMapping.create(..., LocalDateTime now)`

## 3단계: 서비스와 호출부 변경

각 클래스에 `private final Clock clock`을 주입하고 현재 시각을 `LocalDateTime.now(clock)`으로 계산한다. 같은 작업 단위에서 여러 timestamp가 필요하면 한 번 계산한 `now`를 재사용한다.

### 사용자·알림

- [ ] `AccountService`: 토큰 `createdAt`, `isExpired(now)`
- [ ] `NotificationSubscriptionService`: 생성/갱신 timestamp, `disable(now)`, method reference를 lambda로 변경
- [ ] `NotificationTxOperations`: `claimedAt`, `disable(now)`
- [ ] `NotificationService`: `staleBefore`
- [ ] `CommunityAlarmEventListener`: `scheduledAt`
- [ ] `AdminMarketingPushService`: inline `ZoneId` 제거
- [ ] `TodoAlarmScheduler`: inline `ZoneId` 제거
- [ ] `ScheduleAlarmScheduler`: inline `ZoneId` 제거
- [ ] `MedicationAlarmScheduler`: inline `ZoneId` 제거

### 캘린더·커뮤니티·상담

- [ ] `CalendarConnectionService`: inline `ZoneId` 제거, 엔티티 메서드에 `now` 전달
- [ ] `GoogleCalendarClient`: 고정 `SERVICE_ZONE` 제거, `Clock` 기준 시각/zone 사용
- [ ] `CommentService`: `comment.update(..., now)`
- [ ] `CommunityService`: `board.update(..., now)`
- [ ] `ConsultationService`: consultation mutation과 생성 timestamp에 `now` 전달

### 저널·약물·공지

- [ ] `JournalTagCatalogService`: 태그/선호 팩토리와 mutation에 `now` 전달
- [ ] `JournalTagCatalogCheckService`: `checkedAt`, legacy mapping 생성 시 `now` 전달
- [ ] `DefaultTagService`: 선호 생성 시 `now` 전달
- [ ] `MedicationService`: 고정 `SERVICE_ZONE` 제거, 생성/수정 시 `now` 재사용
- [ ] `MedicationAnalysisService`: snapshot timestamp와 `generatedAt`
- [ ] `NoticeService`: `notice.update(..., now)`

### 기타

- [ ] `OnboardingService`: 현재 시각 계산 4곳
- [ ] `SupportInquiryService`: `createdAt`
- [ ] `TermService`: 현재 시각 계산
- [ ] `TodoService`: `createdAt` (전수 검색에서 추가 확인)

## 4단계: 테스트 보정

- [ ] 변경된 엔티티 팩토리/mutation 호출부에 명시적 `now` 전달
- [ ] 생성자에 `Clock`이 추가된 서비스 단위 테스트에 `Clock.fixed(...)` mock/fixture 추가
- [ ] timestamp를 검증하는 테스트는 고정 시각을 기준으로 기대값 보정
- [ ] `adminAuditClock` 관련 테스트와 동작은 변경하지 않음

## 5단계: 검증

- [x] `./gradlew.bat compileJava`
- [x] `./gradlew.bat compileTestJava`
- [x] `./gradlew.bat test`
- [x] 아래 검색 결과가 0건인지 확인

```powershell
rg -n -F "LocalDateTime.now()" src/main/java/attune
```

- [x] `Asia/Seoul` ZoneId 생성은 `AppClockConfig.java` 1곳만 남는지 확인

```powershell
rg -n "ZoneId\.of\([^\r\n]*Asia/Seoul" src/main/java
```

- [x] `adminAuditClock`이 UTC 및 qualifier 기반으로 유지되는지 확인
- [x] `git diff --check`

## 완료 기준

- 모든 production 코드의 무인자 `LocalDateTime.now()`가 제거된다.
- 애플리케이션 시간대 정의가 `AppClockConfig`로 집중된다.
- 엔티티가 시스템 시계에 직접 의존하지 않는다.
- 전체 테스트가 통과하고 컴파일 오류가 없다.
