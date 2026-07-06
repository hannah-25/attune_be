# 실행 계획: HTTP→DB 전 구간 통합 테스트 확충

- 상태: active
- 작성자 / 날짜: hannah / 2026-07-05
- 관련 이슈/PR: (#96 Testcontainers MySQL 전환의 후속) · 0단계 인프라+auth 패턴 검증: #97

## 진행 현황

- 2026-07-06 0단계 인프라 + auth 시나리오 4개 구현 완료, 전체 테스트 그린(4m 6s) → PR #97
- 2026-07-06 auth 시나리오 확장 진행:
  - 소셜 로그인/withdrawn 409/social restore/정지 계정 차단 4개 추가.
  - reissue 만료 access token 허용 경로, 일반 로그인 정지 계정 차단 2개 추가.
  - 중간 실행 1: `./gradlew test --tests "attune.auth.*IntegrationTest"` → 7개 중 1개 실패.
    원인: social restore 테스트가 실제 응답 필드 `status` 대신 `userStatus` 를 기대.
  - 중간 실행 2: 같은 명령 → 10개 중 2개 실패.
    원인: `JwtAuthenticationFilter.shouldNotFilter` 가 `/v1/auth/reissue` 를 안정적으로 제외하지 못해
    만료 access token이 컨트롤러 전 필터에서 401 처리됨. 또한 일반 로그인 정지 계정은
    Spring Security `LockedException` 이 도메인 401로 변환되지 않아 500으로 응답.
  - 수정 후 실행: `./gradlew test --tests "attune.auth.*IntegrationTest"` → 10개 통과, 4m 17s.
- 2026-07-06 journal 시나리오 5개 추가:
  - 사용자 태그 생성 → 체크 → 단일 일지 조회.
  - 타인 사용자 태그 체크 404.
  - 메모 upsert + 수면/식사 upsert 후 단일 일지 조회.
  - 목표 생성 → 점수 기록 → 단일 일지 조회.
  - 중복 사용자 태그 409.
  - 중간 실행 1: `./gradlew test --tests "attune.journal.JournalIntegrationTest"` → 3개 중 2개 실패.
    원인: JsonPath 숫자 응답이 `Integer` 로 반환되는데 테스트 헬퍼가 `Long` 직접 캐스팅.
  - 중간 실행 2: 같은 명령 → 3개 중 2개 실패.
    원인: 요청 바디의 `LocalDate` 를 테스트 ObjectMapper가 직렬화하지 못함. HTTP 요청값을 ISO 날짜 문자열로 교정.
  - 수정 후 단독 실행: `./gradlew test --tests "attune.journal.JournalIntegrationTest"` → 5개 통과, 4m.
  - auth+journal 묶음 실행:
    `./gradlew test --tests "attune.auth.*IntegrationTest" --tests "attune.journal.JournalIntegrationTest"`
    → 15개 통과, 5m 13s.
  - 전체 테스트 실행: `./gradlew test` → 통과, 4m 37s.
    종료 시 Hibernate `ddl-auto: create-drop` FK drop 실패 로그가 출력됐지만 Gradle 결과는 `BUILD SUCCESSFUL`.
- 2026-07-06 medication 시나리오 4개 추가:
  - 표준 약 검색과 상세 조회(Redis JSON 캐시 경로 포함).
  - user-medication 등록 → quick log(TAKEN) → 개별 로그 조회 → 기간 로그 조회.
  - 타인 user-medication 로그 조회/quick log 404.
  - 기간 로그 조회 날짜 역전 400.
  - 단독 실행: `./gradlew test --tests "attune.medication.MedicationIntegrationTest"` → 4개 통과, 3m 22s.
  - auth+journal+medication 묶음 실행:
    `./gradlew test --tests "attune.auth.*IntegrationTest" --tests "attune.journal.JournalIntegrationTest" --tests "attune.medication.MedicationIntegrationTest"`
    → 19개 통과, 3m 44s.
  - 전체 테스트 실행: `./gradlew test` → 통과, 7m 12s.
    종료 시 Hibernate `ddl-auto: create-drop` FK drop 실패 로그가 출력됐지만 Gradle 결과는 `BUILD SUCCESSFUL`.
- 2026-07-06 onboarding 시나리오 3개 추가:
  - status → symptoms → ASRS → AI recommendations(Gemini 목) → goals → complete → status/history/history detail 전체 플로우.
  - skip 후 status skipped 확인.
  - 필수 단계 미완료 complete 400.
  - 중간 실행 1: `./gradlew test --tests "attune.onboarding.OnboardingIntegrationTest"` → 3개 중 1개 실패.
    원인: 응답은 정상이나 MockMvc JSONPath 필터 표현식이 추천 태그 배열을 안정적으로 매칭하지 못함.
    배열 위치 기반 검증으로 교정.
  - 중간 실행 2: 같은 명령 → 3개 중 1개 실패.
    원인: 이력 상세는 ASRS 완료 시각 이전/동시점의 증상 기록을 붙이는데, 테스트 플로우가 ASRS 후 symptoms 순서라
    `symptom: null` 응답. 제품 status 단계(증상 → ASRS)와 맞게 테스트 순서를 교정.
  - 수정 후 단독 실행: `./gradlew test --tests "attune.onboarding.OnboardingIntegrationTest"` → 3개 통과, 3m 21s.
  - auth+journal+medication+onboarding 묶음 실행:
    `./gradlew test --tests "attune.auth.*IntegrationTest" --tests "attune.journal.JournalIntegrationTest" --tests "attune.medication.MedicationIntegrationTest" --tests "attune.onboarding.OnboardingIntegrationTest"`
    → 22개 통과, 4m 10s.
  - 전체 테스트 실행: `./gradlew test` → 통과, 8m 37s.
    종료 시 Hibernate `ddl-auto: create-drop` FK drop 실패 로그가 출력됐지만 Gradle 결과는 `BUILD SUCCESSFUL`.
- 2026-07-06 schedule/todo 시나리오 4개 추가:
  - 카테고리 생성/목록/수정 + 일정 생성/목록/상세/알람 수정/삭제.
  - 타인 일정 상세/알람 수정 404.
  - 일정 알람 4개 초과 400.
  - 투두 생성 → 날짜별 목록 → 상세 → 수정.
  - 중간 실행 1: `./gradlew test --tests "attune.schedule.ScheduleTodoIntegrationTest"` → 4개 중 1개 실패.
    원인: 응답 JSON은 `LocalDateTime` 을 `yyyy-MM-dd'T'HH:mm:ss` 로 직렬화하지만 테스트 기대값은
    `LocalDateTime.toString()` 이라 초가 0일 때 `:00` 이 생략됨. 기대값 포맷을 고정.
  - 수정 후 단독 실행: `./gradlew test --tests "attune.schedule.ScheduleTodoIntegrationTest"` → 4개 통과, 3m 21s.
  - auth+journal+medication+onboarding+schedule/todo 묶음 실행:
    `./gradlew test --tests "attune.auth.*IntegrationTest" --tests "attune.journal.JournalIntegrationTest" --tests "attune.medication.MedicationIntegrationTest" --tests "attune.onboarding.OnboardingIntegrationTest" --tests "attune.schedule.ScheduleTodoIntegrationTest"`
    → 26개 통과, 7m 2s.
  - 전체 테스트 실행: `./gradlew test` → 통과, 6m 35s.
    종료 시 Hibernate `ddl-auto: create-drop` FK drop 실패 로그가 출력됐지만 Gradle 결과는 `BUILD SUCCESSFUL`.
- 2026-07-06 user/account 시나리오 4개 추가:
  - 회원가입 → 이메일 인증 토큰 DB 확인 → 인증 → 로그인.
  - 비밀번호 재설정 요청 → 토큰 검증 → 재설정 완료 → 기존 비밀번호 거부/새 비밀번호 로그인.
  - 설정 조회/수정 + 닉네임/프로필 이미지 수정 + 프로필 조회.
  - 탈퇴 비밀번호 검증 → 탈퇴 상태 로그인 409 → restore → ACTIVE 복구.
  - 중간 실행 1: `./gradlew test --tests "attune.user.AccountIntegrationTest"` → 4개 중 1개 실패.
    원인: 탈퇴 계정 로그인은 인증 실패 401이 아니라 복구 플로우 유도를 위한 409 Conflict가 현재 API 계약.
    기대값을 409로 교정하고 restore 검증까지 확장.
  - 중간 실행 2: 같은 명령 → 4개 중 1개 실패.
    원인: restore 응답 상태 필드는 `userStatus` 가 아니라 `status`. 기대 필드 교정.
  - 수정 후 단독 실행: `./gradlew test --tests "attune.user.AccountIntegrationTest"` → 4개 통과, 2m 37s.
  - auth+journal+medication+onboarding+schedule/todo+user/account 묶음 실행:
    `./gradlew test --tests "attune.auth.*IntegrationTest" --tests "attune.journal.JournalIntegrationTest" --tests "attune.medication.MedicationIntegrationTest" --tests "attune.onboarding.OnboardingIntegrationTest" --tests "attune.schedule.ScheduleTodoIntegrationTest" --tests "attune.user.AccountIntegrationTest"`
    → 30개 통과, 4m 31s.
  - 전체 테스트 실행: `./gradlew test` → 통과, 6m 17s.
    종료 시 Hibernate `ddl-auto: create-drop` FK drop 실패 로그가 출력됐지만 Gradle 결과는 `BUILD SUCCESSFUL`.
- 2026-07-06 admin 시나리오 4개 추가:
  - USER 롤의 `/v1/admin/**` 접근 403.
  - 관리자 회원 목록 조회 → 회원 상태 SUSPENDED 변경 → 감사 로그 조회.
  - 관리자 공지 생성/수정/삭제 + 공개 공지 목록/상세 조회.
  - 관리자 약관 등록/목록 조회.
  - 단독 실행: `./gradlew test --tests "attune.admin.AdminIntegrationTest"` → 4개 통과, 2m 50s.
  - auth+journal+medication+onboarding+schedule/todo+user/account+admin 묶음 실행:
    `./gradlew test --tests "attune.auth.*IntegrationTest" --tests "attune.journal.JournalIntegrationTest" --tests "attune.medication.MedicationIntegrationTest" --tests "attune.onboarding.OnboardingIntegrationTest" --tests "attune.schedule.ScheduleTodoIntegrationTest" --tests "attune.user.AccountIntegrationTest" --tests "attune.admin.AdminIntegrationTest"`
    → 34개 통과, 5m 1s.
  - 전체 테스트 실행: `./gradlew test` → 통과, 9m 31s.
    종료 시 Hibernate `ddl-auto: create-drop` FK drop 실패 로그가 출력됐지만 Gradle 결과는 `BUILD SUCCESSFUL`.
- 2026-07-06 community/consultation 시나리오 3개 추가:
  - 게시글 생성/목록/상세/수정/삭제 + 댓글 생성/목록/수정/삭제, 작성자/타인 권한 경계 검증.
  - 상담 일정 생성 → 질문 추가/조회/삭제 → 결과 작성/조회/삭제 → 기간 조회 → 일정 수정.
  - 타인 상담 조회 403 + 상담 기간 역전 400.
  - 중간 실행 1: `./gradlew test --tests "attune.communityBoard.CommunityConsultationIntegrationTest"` → 3개 중 2개 실패.
    원인: 서비스 계층에서 던진 Spring Security `AccessDeniedException` 이 전역 예외 처리에 매핑되지 않아
    타인 게시글 수정/타인 상담 조회가 403 대신 500으로 응답. 또한 community 삭제 후 조회 경로의
    `NoSuchElementException` 도 404로 매핑되지 않을 가능성을 함께 확인.
  - 수정: `GlobalExceptionHandler` 에 `AccessDeniedException` → 403, `NoSuchElementException` → 404 매핑 추가.
  - 수정 후 단독 실행: `./gradlew test --tests "attune.communityBoard.CommunityConsultationIntegrationTest"` → 3개 통과, 5m 24s.
  - auth+journal+medication+onboarding+schedule/todo+user/account+admin+community/consultation 묶음 실행:
    `./gradlew test --tests "attune.auth.*IntegrationTest" --tests "attune.journal.JournalIntegrationTest" --tests "attune.medication.MedicationIntegrationTest" --tests "attune.onboarding.OnboardingIntegrationTest" --tests "attune.schedule.ScheduleTodoIntegrationTest" --tests "attune.user.AccountIntegrationTest" --tests "attune.admin.AdminIntegrationTest" --tests "attune.communityBoard.CommunityConsultationIntegrationTest"`
    → 37개 통과, 6m 28s.
  - 전체 테스트 실행: `./gradlew test` → 통과, 10m 25s.
    종료 시 Hibernate `ddl-auto: create-drop` FK drop 실패 로그가 출력됐지만 Gradle 결과는 `BUILD SUCCESSFUL`.
- 2026-07-06 기타 도메인 시나리오 4개 추가:
  - support inquiry 생성 + DB 저장 검증 + 이메일 형식 검증 400.
  - ai/generate → `AiTextGenerator` mock 경로 검증 + 빈 prompt 400.
  - Google Calendar 연결 → 연결 목록 → 동기화(Google client mock) → 외부 이벤트 조회 → 연결 해제.
  - medicationAnalysis: 7일치 복약/일지 상태 기록 기반 availability/summary 조회 → AI 분석 동의 →
    Gemini report client mock 리포트 생성 → 단건/목록 조회 → 동일 데이터 재생성 시 기존 리포트 재사용 →
    최소 7일 미만 기간 400.
  - 중간 실행 1: `./gradlew test --tests "attune.misc.MiscIntegrationTest"` → `compileTestJava` 실패.
    원인: `/v1/ai-analysis-consent` 호출에 필요한 MockMvc `put` request builder static import 누락.
  - 수정 후 단독 실행: `./gradlew test --tests "attune.misc.MiscIntegrationTest"` → 4개 통과, 3m 5s.
  - 전체 신규 통합 테스트 묶음 실행:
    `./gradlew test --tests "attune.auth.*IntegrationTest" --tests "attune.journal.JournalIntegrationTest" --tests "attune.medication.MedicationIntegrationTest" --tests "attune.onboarding.OnboardingIntegrationTest" --tests "attune.schedule.ScheduleTodoIntegrationTest" --tests "attune.user.AccountIntegrationTest" --tests "attune.admin.AdminIntegrationTest" --tests "attune.communityBoard.CommunityConsultationIntegrationTest" --tests "attune.misc.MiscIntegrationTest"`
    → 41개 통과, 5m 14s.
  - 전체 테스트 실행: `./gradlew test` → 통과, 8m 16s.
    종료 시 Hibernate `ddl-auto: create-drop` FK drop 실패 로그가 출력됐지만 Gradle 결과는 `BUILD SUCCESSFUL`.
- 2026-07-06 현재 상태: 우선순위 1~9 도메인 HTTP→DB 통합 테스트 구현 및 로컬 검증 완료.
  머지/CI 안정 통과 확인은 아직 남음.
- 2026-07-06 작업 후 문서 반영:
  - `docs/engineering/testing-strategy.md` 에 41개 HTTP→DB 통합 테스트 범위와 `IntegrationTest` 사용 규칙 갱신.
  - `docs/quality/quality-score.md` 의 테스트 점수 3 → 4로 재평가. CI 연속 안정성/커버리지 미측정은 잔여 리스크로 유지.
- 2026-07-06 문서 점검:
  - `bash scripts/agent/check-docs` 는 현재 Windows `bash` 가 WSL로 연결되고 설치된 WSL 배포판이 없어 실행 실패.
  - 같은 로직을 PowerShell로 재현해 점검: 깨진 상대 링크 0건, `docs/generated/project-map.md`,
    `docs/generated/api-index.md`, `docs/generated/data-schema.md` 모두 존재.
- 2026-07-06 PR 전 안전 점검:
  - 신규 통합 테스트 파일에서 `@Disabled`, TODO/FIXME, 임시 출력, `Thread.sleep`, 테스트 스킵 흔적 없음.
  - `src/main/resources/application-secret.yml.bak` 백업 파일이 untracked로 노출되어 있어 `.gitignore` 를
    `src/main/resources/application-secret.yml*` 로 보강. 파일 내용은 수정하지 않음.
  - `git diff --check` 통과(공백 오류 없음, Windows 줄끝 변환 경고만 출력).
- 다음: 최종 diff 점검 및 PR 준비.

## 목표

실제 HTTP 요청 → 시큐리티 필터(실제 JWT) → 컨트롤러 → 서비스 → JPA → **Testcontainers MySQL/Redis**
전 구간을 지나는 통합 테스트를 도메인별로 갖춘다. 완료되면:

- 배포 전 "엔드포인트가 진짜 동작하는가"를 CI에서 자동 검증한다.
- OSIV off 환경의 지연 로딩·flush 순서·DB 제약 이슈가 목 테스트에 가려지지 않는다.
- 인증 캐시(`UserAuthCacheRepository`)·JSON 캐시(`RedisJsonCache`) 경로가 실제 Redis로 검증된다.

## 배경

- #96 으로 테스트 DB가 Testcontainers MySQL 8.4로 전환됐지만, 이를 활용하는 테스트는
  `UserDataDeletionExecutorIntegrationTest`(`@DataJpaTest`) 1개뿐이다.
- 컨트롤러 테스트는 전부 `@WebMvcTest` + 서비스 목이라 HTTP→DB 전 구간을 지나는 테스트가 없다.
- [testing-strategy](../../engineering/testing-strategy.md)에서 "통합테스트 시나리오 확충은 별도 계획으로
  진행"으로 남겨둔 그 계획이 이 문서다.

## 초기 상태

- `src/test/resources/application.yml`: `jdbc:tc:mysql:8.4` + `TC_DAEMON=true`, `ddl-auto: create-drop`.
  Redis는 `localhost:6379`를 바라봄(컨테이너 아님) — 전 구간 테스트 시 인증 캐시가 실제 Redis를 침.
- `@SpringBootTest` 전체 컨텍스트 기동 확인됨(`AttuneApplicationTests`).
- Redis 사용처: `common/cache/RedisJsonCache`, `auth/domain/repository/UserAuthCacheRepository`.
- 외부 연동: Gemini(ai/onboarding/medicationAnalysis), 메일, 웹푸시, Google(OAuth/Calendar), Apple.

## 현재 구현 상태

- `IntegrationTest` 베이스에서 Redis Testcontainer(`redis:7-alpine`)를 기동하고
  `@DynamicPropertySource` 로 `spring.data.redis.host/port` 를 주입한다.
- 테스트 간 DB는 `DatabaseCleaner`, Redis는 `RedisCleaner` 로 격리한다.
- 외부 연동은 베이스 mock/stub으로 격리하고, OAuth verifier 라우팅 mock은 매 테스트마다 기본 stubbing한다.
- 우선순위 1~9 핵심 시나리오 41개가 로컬에서 통과했고, 전체 `./gradlew test` 도 통과했다.

## 변경 범위

### 0단계 — 공통 인프라 (선행)

1. **Redis Testcontainer**: `redis:7-alpine` `GenericContainer` 를 static 싱글턴으로 기동하고
   `@DynamicPropertySource` 로 `spring.data.redis.host/port` 주입. MySQL처럼 JVM 단위 재사용.
2. **`IntegrationTest` 추상 베이스** (`src/test/java/attune/support/IntegrationTest.java`):
   - `@SpringBootTest` + `@AutoConfigureMockMvc`
   - 외부 연동 `@MockitoBean` 대상을 **타입 단위로 고정** (베이스 클래스 한 곳에만 선언):

     | Mock 타입 | 실제 구현 | 사용처 |
     |---|---|---|
     | `AiTextGenerator` | `GeminiTextGenerator` | ai, onboarding |
     | `GeminiReportClient` | (자체) | medicationAnalysis |
     | `GoogleCalendarClient` | (자체) | calendar |
     | `OAuthVerifier` ×3 | `GoogleOAuthVerifier`, `AppleOAuthVerifier`, `KakaoOAuthVerifier` | auth (구현체별 개별 mock) |
     | `JavaMailSender` | — | account 메일 |

   - **푸시는 mock 불필요**: `notification.push.provider` 미설정 시 `StubPushSender` 가 활성
     (`@ConditionalOnProperty(matchIfMissing = true)`)이고 `supports()` 항상 true + 로그만 남긴다.
     테스트 yml이 이 프로퍼티를 설정하지 않으므로 stub이 그대로 쓰인다.
   - **라우팅용 mock은 베이스 클래스에서 기본 stubbing 필수** (mock 기본 반환값이 라우팅을 깨뜨림):
     - `OAuthVerifier` mock 3개는 각각 `provider()` 가 `GOOGLE`/`APPLE`/`KAKAO` 를 반환하도록 stubbing —
       `SocialAuthService` 가 `v.provider() == request.provider()` 로 구현체를 고르는데 mock 기본값은 null.
       `@MockitoBean` 은 테스트마다 리셋되므로 stubbing은 `@BeforeEach` 에서 매번 건다.
3. **테스트 설정 정합성 수정**: `src/test/resources/application.yml` 의 `ai.gemini.*` 는 실제 바인딩
   prefix(`gemini.*`, `GeminiProperties`)와 불일치 — 0단계에서 `gemini.api-key`, `gemini.base-url`,
   `gemini.model` **세 값 모두** 로 교정한다. `GeminiConfig` 가 `properties.baseUrl()` 로 `RestClient` 를
   만들므로 `base-url` 누락 시 빈 생성이 깨질 수 있다.
4. **`DatabaseCleaner`**: JPA 엔티티 목록이 아닌 **`information_schema.tables` 기반**으로 대상 테이블을
   수집해 truncate (`FOREIGN_KEY_CHECKS=0` 감싸기). 수집 조건은 `TABLE_SCHEMA = DATABASE() AND
   TABLE_TYPE = 'BASE TABLE'` 로 한정해 뷰/타 스키마 오염을 배제한다.
   `asrs_answers` 같은 `@ElementCollection` 테이블 누락 방지.
   `@Transactional` 롤백 방식은 커밋 시점 이슈를 가리므로 채택하지 않는다.
5. **`RedisCleaner`**: `FLUSHALL` 대신 **`FLUSHDB`** 를 쓰고, 실행 전 커넥션 host/port가 Testcontainer의
   것과 일치하는지 assert — `@DynamicPropertySource` 주입 실패 시 로컬 `localhost:6379` 를 지우는 사고 방지.
6. **테스트 픽스처 헬퍼**: 테스트 유저 생성 + **실제 JWT 발급**(`JwtProvider`) →
   `Authorization: Bearer` 헤더 부착 유틸. 목 인증(`.with(user())`)은 통합 테스트에서 쓰지 않는다.
7. **`ReferenceDataFixture`**: `ddl-auto: create-drop` + seed SQL 부재이므로 표준 약(medications)·약관(terms)
   등 기준 데이터를 넣는 공통 픽스처를 0단계에 포함 — 이후 도메인 PR이 각자 임기응변하지 않도록.

### 1단계~ — 도메인별 시나리오 (우선순위 순, 도메인당 별도 PR)

각 도메인은 기본 3종 세트 — **정상 플로우 / 인가 실패(401·403·타인 리소스) / 검증·제약 위반(400)** —
를 갖추고, 전 엔드포인트 나열보다 핵심 시나리오 3~5개로 시작한다.

| 순위 | 도메인 | 핵심 시나리오 |
|---|---|---|
| 1 | auth | 로그인→토큰 발급, reissue(회전·만료 토큰 거부), **logout 후 reissue 거부**(refresh 캐시 삭제 확인 — access token은 stateless라 만료까지 유효, 아래 제외 범위 참고), 소셜 로그인(검증기 목), 인증 캐시 적중 경로 |
| 2 | journal | 태그 생성→체크→조회 전 구간, 목표 CRUD, 메모/수면·식사 upsert, 타인 태그 접근 차단, 중복 태그 제약 |
| 3 | medication | user-medication 등록→quick log→기간 로그 조회(scheduleId #94 포함), 표준 약 조회(Redis JSON 캐시 적중/미스), 타인 약 접근 차단 |
| 4 | onboarding | asrs→symptoms→ai-recommendations(Gemini 목)→goals→complete 전체 플로우, skip, status/history |
| 5 | schedule / todo | 일정 CRUD + 알람 PUT(per-user timezone #95 반영), 카테고리 CRUD, 투두 CRUD |
| 6 | user / account | signup→이메일 인증(메일 목), 비밀번호 재설정 플로우, withdraw→restore, 프로필/닉네임/설정 |
| 7 | admin | 멤버 목록/상태 변경/탈퇴 처리(감사 로그 기록 검증 포함), 공지 CRUD+푸시(목), 약관 등록, USER 롤 403 |
| 8 | community / consultation | 게시글·댓글 CRUD, 상담 생성→질문→결과 플로우 |
| 9 | 기타 | medicationAnalysis(리포트 생성, Gemini 목), notice 조회, support 접수, calendar 연결(Google 목), ai/generate |

## 제외 범위

- **access token blacklist**: 현재 logout은 Redis refresh 캐시만 삭제하고
  (`AuthService.logout`), 요청 인증은 Redis 조회 없이 JWT 서명·만료만 검증한다
  (`JwtAuthenticationFilter`). 즉 logout 후에도 access token은 만료까지 유효한 것이
  **현재 설계**다. 이를 바꾸는 blacklist 도입은 보안 설계 변경이므로 이 계획에서 다루지
  않고 별도 결정 사항으로 남긴다.
- 스케줄러(`*AlarmScheduler`) 트리거 자체의 E2E — 기존 서비스 테스트 유지.
- 성능/부하 테스트, JaCoCo 도입(별도 tech-debt TD-6).
- 실제 외부 API 호출(Gemini/메일/푸시/Google/Apple)은 항상 목.
- 기존 `@WebMvcTest`/서비스 테스트의 통합 테스트 전환(대체가 아니라 보완).

## 관련 문서

- [testing-strategy](../../engineering/testing-strategy.md)
- [tech-debt-tracker](../tech-debt-tracker.md)
- [api-index](../../generated/api-index.md), [db_schema](../../db_schema.md)

## 관련 코드

- `src/test/resources/application.yml` — 테스트 데이터소스/Redis 설정
- `src/test/java/attune/user/application/UserDataDeletionExecutorIntegrationTest.java` — 기존 유일 통합 테스트
- `src/main/java/attune/common/util/JwtProvider.java` — 실 JWT 발급 헬퍼가 사용
- `src/main/java/attune/auth/domain/repository/UserAuthCacheRepository.java` — Redis 인증 캐시
- `src/main/java/attune/common/cache/RedisJsonCache.java` — Redis JSON 캐시
- `src/main/java/attune/auth/application/AuthService.java` — logout은 refresh 캐시 삭제만 수행
- `src/main/java/attune/common/filter/JwtAuthenticationFilter.java` — 인증은 JWT 자체 검증(stateless)
- `src/main/java/attune/ai/config/GeminiProperties.java` — prefix `gemini.*` (테스트 yml 교정 대상)
- `src/main/java/attune/onboarding/domain/model/AsrsAssessment.java` — `asrs_answers` `@ElementCollection` (클리너 누락 주의)

## 작업 단계

1. 0단계 인프라 PR: Redis 컨테이너 + `IntegrationTest` 베이스 + 클리너 + JWT 헬퍼
   + auth 시나리오 1~2개로 패턴 검증
2. auth 시나리오 완성 PR
3. journal PR → medication PR → onboarding PR → … (표의 우선순위 순, 도메인당 1 PR)
4. 각 PR마다 CI 소요 시간 확인 — 전체 테스트 10분 초과 시 병렬화/시나리오 조정 논의

## 검증 방법

- `scripts/agent/test` 전체 통과 (Docker 필수 — MySQL + Redis 컨테이너)
- 신규 통합 테스트가 목 인증 없이 실제 JWT로 통과하는지 확인
- 테스트 단독 실행/전체 실행 결과 동일(격리 검증): `./gradlew test --tests "attune.<도메인>.*IntegrationTest"`

## 위험 요소

- **CI 시간 증가**: 컨테이너 2개 + 전체 컨텍스트 기동. 컨텍스트 캐시가 깨지지 않도록
  `@MockitoBean` 구성을 베이스 클래스 한 곳으로 고정한다(테스트별 상이한 목 구성은 컨텍스트 재기동 유발).
- **테스트 간 간섭**: `create-drop`은 JVM당 1회라 데이터가 누적됨 → `DatabaseCleaner` 필수.
- **flaky**: 시간 의존 로직은 고정 클록, 비동기(@Async) 경로는 Awaitility 등으로 명시 대기.

## 롤백 방법

- 테스트 코드와 테스트 의존성만 추가하므로 main 소스 영향 없음. 해당 PR revert로 충분.

## 의사결정 로그

- 2026-07-05 Redis는 스텁이 아닌 **Testcontainer 실 컨테이너** 채택 — 인증·캐시 경로까지 검증, Docker는 이미 필수 환경 (사용자 결정)
- 2026-07-05 계획 범위는 **전 도메인 로드맵 확정** — PR은 도메인당 분할 (사용자 결정)
- 2026-07-05 데이터 격리는 `@Transactional` 롤백 대신 **테이블 클린업** — OSIV off 커밋 시점 이슈를 가리지 않기 위함
- 2026-07-05 리뷰 반영: auth 시나리오를 "logout 후 접근 차단"→"logout 후 reissue 거부"로 수정(access token은 stateless), mock 대상 빈 타입 명시, DatabaseCleaner는 information_schema 기반, Redis는 FLUSHDB+컨테이너 가드, ReferenceDataFixture와 `gemini.*` prefix 교정을 0단계에 추가
- 2026-07-05 2차 리뷰 반영: 라우팅용 mock 기본 stubbing 명시(`OAuthVerifier.provider()`, `PushSender.supports()`), `gemini.base-url` 포함 3개 키 교정, 클리너 수집 조건 `DATABASE()`+`BASE TABLE` 한정, 0단계 완료 조건을 실제 산출물 목록과 일치시킴
- 2026-07-06 구현 중 확인: `PushSender` mock은 불필요 — `StubPushSender` 가 기본 활성(matchIfMissing=true)이라 실제 stub 빈을 그대로 사용. mock 목록에서 제거
- 2026-07-06 `JavaMailSender` 인터페이스 mock이 actuator mail health indicator(`JavaMailSenderImpl` 구체 타입 요구)와 충돌해 컨텍스트 기동 실패 → 테스트 yml에 `management.health.mail.enabled: false` 추가로 해결

## 완료 조건

- [ ] 0단계 인프라 머지 — `IntegrationTest` 베이스(외부 mock + 기본 stubbing 포함)·Redis 컨테이너·
      `DatabaseCleaner`·`RedisCleaner`(컨테이너 가드)·JWT 헬퍼·`ReferenceDataFixture`·테스트 yml `gemini.*` 교정
- [ ] 우선순위 1~5 도메인(auth·journal·medication·onboarding·schedule/todo) 시나리오 머지
- [ ] 우선순위 6~9 도메인 시나리오 머지
- [ ] 전체 테스트 CI 안정 통과(연속 3회 flaky 없음)

## 작업 후 문서 업데이트 목록

- [x] `docs/engineering/testing-strategy.md` — 통합 테스트 레이어·베이스 클래스 사용법 추가
- [x] `docs/quality/quality-score.md` — 테스트 항목 점수 재평가
- [ ] 완료 시 이 문서를 `completed/` 로 이동
