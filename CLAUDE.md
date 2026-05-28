# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Spring Boot 4 / Java 17 기반의 헥사고날 아키텍처 백엔드 프로젝트.

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

헥사고날(포트 & 어댑터) 구조. 모든 도메인 모듈은 동일한 패키지 레이아웃을 따른다:

```
attune/<domain>/
  adapter/web/      ← REST 컨트롤러
  application/      ← 서비스 + DTO
  domain/
    model/          ← JPA 엔티티, 열거형
    repository/     ← Spring Data JPA 인터페이스
```

### 도메인 모듈

| 모듈 | 역할 |
|------|------|
| `auth` | JWT 발급·갱신·로그아웃, Redis 토큰 캐시 (`UserAuthCache`) |
| `user` | 계정 관리, 설정, 이메일 인증, 비밀번호 재설정 |
| `term` | 약관 버전 관리 및 사용자 동의 이력 |
| `journal` | 일지: Condition/SideEffect/Trouble 태그 + 일별 체크인 로그, DailyStatus, Memo, DailyGoal |
| `medication` | 약물 마스터, 사용자 복약 정보, 스케줄, 복약 로그 |
| `consultation` | 진료 일정·기록 관리 |
| `notice` | 공지사항 |
| `communityBoard` | 커뮤니티 게시글·댓글 (`feat/board` 브랜치에서 개발 중) |
| `common` | `SecurityConfig`, `JwtAuthenticationFilter`, `JwtProvider`, `GlobalExceptionHandler`, 공유 유틸 |

## 주요 패턴

### 현재 사용자 조회

서비스에서 로그인 사용자 UUID를 얻을 때는 `SecurityUtils.getCurrentUserUuid()`를 사용한다. 컨트롤러가 아닌 서비스 레이어에서 호출한다.

### 에러 처리

`common/error`에 HTTP 상태별 기반 예외 클래스가 있다:
- `BadRequestException` → 400
- `UnauthorizedException` → 401
- `NotFoundException` → 404
- `ConflictException` → 409

도메인별 구체 예외는 `badrequest/`, `notfound/`, `conflict/`, `unauthorized/` 하위 패키지에 위치하며 해당 기반 클래스를 상속한다. 새 예외를 추가할 때는 같은 패턴을 따른다.

### Journal 모듈 데이터 모델 관례

`*Tag` 엔티티 (e.g. `ConditionTag`, `SideEffectTag`) — 사용자가 등록한 활성 태그 목록.  
`*Log` 엔티티 (e.g. `ConditionLog`, `SideEffectLog`) — 날짜별 실제 체크인 기록.  
일지 조회는 두 계층을 JOIN해서 반환하며, Repository에 `findAllInRangeWithTag` 형태의 커스텀 쿼리가 사용된다.

### OSIV 비활성화

`open-in-view`가 꺼져 있다. 트랜잭션 내에서 필요한 연관 데이터를 모두 조회해야 한다. 서비스 메서드 밖에서 지연 로딩을 시도하면 예외가 발생한다.

## 작업 규칙

### 새 테이블/엔티티 생성 전

새 JPA 엔티티 또는 테이블을 추가하기 전에 반드시 `docs/db_schema.md`를 먼저 확인한다.
- 추가하려는 테이블이 이미 정의되어 있으면 → 정의된 스키마를 그대로 따른다.
- 정의와 다르거나(컬럼 추가/타입 변경/제약 변경 등) 문서에 없는 새 테이블이면 → 작업 전 사용자에게 변경 내용을 알리고 확인을 받는다.

### 작업 후 문서 갱신

코드/구조를 변경한 작업이 끝나면 영향받는 문서를 같은 흐름에서 갱신한다. 매핑은 다음과 같다.

- 도메인 모듈 추가/제거/이름 변경 → `docs/architecture.md` + `CLAUDE.md` 도메인 모듈 표
- 엔티티/컬럼 추가·변경 → `docs/db_schema.md`
- 인증·토큰·시크릿 흐름 변경 → `docs/security.md`
- 프로파일·DB·JPA 설정 변경 → `docs/database.md`
- 비동기·메일 변경 → `docs/async-mail.md`
- 새 TODO/수정 거리 발견 → `docs/notes.md`
- API 엔드포인트 추가·변경·삭제 → `docs/api-guide.md`
- 빌드·실행·테스트 명령어 변경 → `CLAUDE.md` 명령어 섹션

## 참고 문서

| 문서 | 내용 |
|------|------|
| [`docs/architecture.md`](./docs/architecture.md) | 헥사고날 구조, 도메인 모듈 목록 |
| [`docs/security.md`](./docs/security.md) | JWT 인증/토큰 흐름, 시크릿 관리 |
| [`docs/database.md`](./docs/database.md) | DB 프로파일, JPA 설정, 운영 규칙 |
| [`docs/db_schema.md`](./docs/db_schema.md) | 전체 테이블·컬럼 스키마 |
| [`docs/async-mail.md`](./docs/async-mail.md) | 비동기 처리 및 메일 발송 |
| [`docs/notes.md`](./docs/notes.md) | 개발 중 발견된 수정/개선 TODO |
| [`docs/api-guide.md`](./docs/api-guide.md) | Journal API 엔드포인트 명세 (프론트 연동용) |
