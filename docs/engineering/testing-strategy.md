# 테스트 전략

## 현재 상태

- 테스트 프레임워크: JUnit 5 + Spring Boot Test + Spring Security Test.
- 테스트는 도메인 구조를 미러링한다(`src/test/java/attune/<domain>/...`).
- 테스트 DB: **Testcontainers MySQL 8.4** — `src/test/resources/application.yml` 의 `jdbc:tc:` URL이
  첫 커넥션 시 컨테이너를 자동 기동하고 JVM 종료까지 재사용한다(`TC_DAEMON=true`). **Docker 필수**.
- JVM 타임존은 Asia/Seoul 고정(`build.gradle` 의 `-Duser.timezone`).
- main 452 / test 53 파일 (2026-06 기준). application·domain 레이어 중심으로 커버리지 존재.
- 2026-07-06 기준 HTTP→DB 전 구간 통합 테스트 41개가 추가되어 auth, journal, medication,
  onboarding, schedule/todo, user/account, admin, community/consultation, support, calendar,
  ai/generate, medicationAnalysis 핵심 시나리오를 검증한다.

## 테스트 레이어 가이드

| 종류 | 대상 | 예 |
|------|------|----|
| 단위 테스트 | DTO 검증, 도메인 모델 불변식, 순수 로직 | `UserTest`, `UpdateMedicationRequestTest` |
| 서비스 테스트 | 유스케이스(목 Repository/협력자) | `JournalServiceTest`, `MedicationServiceTest` |
| Repository 테스트 | 커스텀 쿼리 | `AdminMemberRepositoryTest`, `AdminAuditLogRepositoryTest` |
| 컨트롤러/보안 테스트 | 엔드포인트·인가 | `AdminMemberSecurityTest`, `JournalTagControllerTest` |
| **통합 테스트** | HTTP→필터(실 JWT)→서비스→JPA→MySQL/Redis 전 구간 | `AuthIntegrationTest`, `MiscIntegrationTest` |
| 아키텍처 테스트 | 계층/레이아웃 규칙 | `attune.architecture.*Test` (ArchUnit) |

### 통합 테스트 작성법

`attune.support.IntegrationTest` 를 상속한다 (`*IntegrationTest` 네이밍, 도메인 패키지에 배치).

- 베이스가 제공: `mockMvc`, `objectMapper`, `testUsers`(유저 생성 + 실 JWT 발급),
  `referenceData`(표준 약·복약 용량·약관·시스템 일지 태그 기준 데이터),
  테스트 간 DB truncate + Redis FLUSHDB 자동 실행.
- 인증은 목이 아니라 실제 토큰: `testUsers.bearer(user)` 를 `Authorization` 헤더에 태운다.
- 외부 연동 mock(`aiTextGenerator` 등)은 **베이스에만** 선언돼 있다. 서브클래스에서
  `@MockitoBean` 을 추가하면 컨텍스트 캐시가 깨져 CI가 느려지므로 금지 — 필요하면 베이스에 추가.
- Redis는 Testcontainers(`redis:7-alpine`)로 자동 기동. MySQL과 함께 **Docker 필수**.
- 현재 통합 테스트는 외부 API를 직접 호출하지 않는다. Gemini, Google Calendar, OAuth, JavaMailSender는
  베이스 mock/stub으로 격리하고, 푸시는 기본 `StubPushSender` 를 그대로 사용한다.
- 확충 로드맵과 실행 결과: [실행 계획](../exec-plans/active/2026-07-05-http-db-integration-tests.md)

## 규칙

1. 새 기능·버그 수정은 **회귀 테스트와 함께** 제출한다.
2. OSIV가 꺼져 있으므로 지연 로딩 경로는 서비스/Repository 테스트로 검증한다.
3. 외부 연동(Gemini/메일/푸시)은 목/스텁으로 격리한다.
4. 시간 의존 로직은 고정 타임존/클록으로 테스트한다.

## 실행

```bash
scripts/agent/test                       # 전체
./gradlew test --tests "attune.Xxx"      # 단일
./gradlew test --tests "attune.Xxx.method"
```

## 개선 후보 (tech-debt)

- 커버리지 측정 도구(JaCoCo) 미도입 → [tech-debt-tracker](../exec-plans/tech-debt-tracker.md).
- ~~통합 테스트(Testcontainers MySQL) 도입 검토.~~ → 2026-07-05 도입 완료.
- ~~HTTP→DB 전 구간 통합테스트 시나리오 확충.~~ → 2026-07-06 우선순위 1~9 핵심 시나리오
  로컬 검증 완료. 머지/CI 안정성 추적은 [실행 계획](../exec-plans/active/2026-07-05-http-db-integration-tests.md)에 기록.
