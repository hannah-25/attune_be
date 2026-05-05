# CLAUDE.md

이 파일은 Claude Code(claude.ai/code)가 이 저장소에서 작업할 때 참고하는 안내 문서입니다.

## 명령어

```bash
# 빌드
./gradlew build
./gradlew clean build

# 실행 (기본 프로파일: local — H2 인메모리 DB 사용)
./gradlew bootRun
./gradlew bootRun --args='--spring.profiles.active=dev'

# 테스트
./gradlew test
./gradlew test --tests "attune.SomeTestClass"
./gradlew test --tests "attune.SomeTestClass.specificMethod"
```

로컬 실행 시 Swagger UI는 `http://localhost:8080/swagger-ui.html` 에서 확인할 수 있다.

## 아키텍처

Spring Boot 4 / Java 17 기반의 **헥사고날(포트 & 어댑터) 아키텍처** 애플리케이션이다. 모든 도메인 모듈은 동일한 내부 구조를 따른다:

```
attune/<domain>/
  domain/
    adapter/web/      ← REST 컨트롤러 (HTTP 인바운드 어댑터)
    application/      ← 서비스 클래스 + DTO (유스케이스 레이어)
    model/            ← JPA 엔티티 및 열거형
    repository/       ← Spring Data JPA 인터페이스 (아웃바운드 포트)
```

### 모듈

| 모듈 | 역할 |
|------|------|
| `auth` | JWT 발급, 갱신, 로그아웃; Redis 기반 토큰 캐시 |
| `user` | 계정 관리, 설정, 이메일 인증, 비밀번호 재설정 |
| `term` | 약관 버전 관리 및 사용자 동의 이력 추적 |
| `communityBoard` | 커뮤니티 게시글 (`feat/board` 브랜치에서 개발 중) |
| `common` | 공통 관심사: `SecurityConfig`, `JwtAuthenticationFilter`, `JwtProvider`, `GlobalExceptionHandler`, 공유 유틸 |

### 보안 및 토큰 흐름

- **Stateless JWT** — 서버 측 세션 없음.
- `JwtAuthenticationFilter`가 컨트롤러 도달 전 매 요청마다 액세스 토큰을 검증한다.
- 액세스 토큰은 `Authorization: Bearer …` 헤더로 전달되고, 리프레시 토큰은 **HttpOnly 쿠키**에 저장된다.
- 두 토큰 모두 TTL을 설정해 **Redis**에 보관한다 (`UserAuthCache` 참고).
- JWT 시크릿, DB 자격증명 등 민감 정보는 `application-secret.yml`에 관리하며, 이 파일은 git에서 제외된다. 로컬 환경 구성 시 팀 공유 템플릿을 복사해 실제 값을 채워야 한다.

### 데이터베이스

- `dev`/`prod` 프로파일은 **MySQL 8.4**, `local` 및 테스트는 **H2 인메모리** DB를 사용한다.
- JPA DDL auto는 프로파일별로 설정된다 (dev: `update`, prod: `validate`).
- OSIV(`open-in-view`)는 **비활성화** — 서비스에서 트랜잭션 내에 필요한 모든 데이터를 조회해야 한다.
- `User` 엔티티의 기본 키는 **UUID** 문자열이다.

### 설정 프로파일

| 프로파일 | DB | 비고 |
|----------|-----|------|
| `local` | H2 | 기본값; 디버그 로깅; localhost CORS |
| `dev` | MySQL | 원격 개발 서버; 디버그 로깅 |
| `prod` | MySQL | HTTPS 필수; Actuator 접근 제한 |

민감 정보는 `application-secret.yml`(gitignore 처리됨)을 통해 주입된다. `dev`/`prod` 실행 전 팀 공유 템플릿을 복사해 값을 채워야 한다.

### 비동기 처리 & 메일

이메일 발송은 `@Async`(`@EnableAsync`로 활성화)를 사용한다. `JavaMailSender`는 Gmail SMTP로 설정되어 있으며, 회원가입 후 웰컴 메일은 `ApplicationEventPublisher`를 통해 발행된다.

