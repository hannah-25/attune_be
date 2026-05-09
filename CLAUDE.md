# CLAUDE.md

이 파일은 Claude Code(claude.ai/code)가 이 저장소에서 작업할 때 참고하는 안내 문서입니다.

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

## 작업 규칙

### 새 테이블/엔티티 생성 전

새 JPA 엔티티 또는 테이블을 추가하기 전에 반드시 `docs/db_schema.md`를 먼저 확인한다.
- 추가하려는 테이블이 이미 정의되어 있으면 → 정의된 스키마를 그대로 따른다.
- 정의와 다르거나(컬럼 추가/타입 변경/제약 변경 등) 문서에 없는 새 테이블이면 → 작업 전 사용자에게 변경 내용을 알리고 확인을 받는다.

### 작업 후 문서 갱신

코드/구조를 변경한 작업이 끝나면 영향받는 문서를 같은 흐름에서 갱신한다. 매핑은 다음과 같다.

- 도메인 모듈 추가/제거/이름 변경 → `docs/architecture.md`
- 엔티티/컬럼 추가·변경 → `docs/db_schema.md`
- 인증·토큰·시크릿 흐름 변경 → `docs/security.md`
- 프로파일·DB·JPA 설정 변경 → `docs/database.md`
- 비동기·메일 변경 → `docs/async-mail.md`
- 새 TODO/수정 거리 발견 → `docs/notes.md`
- 새 문서 추가, 기존 문서 분리·통합 → `CLAUDE.md` 참고 문서 표
- 빌드·실행·테스트 명령어 변경 → `CLAUDE.md` 명령어 섹션

## 참고 문서

세부 내용은 아래 문서를 참고할 것.

| 문서 | 내용 |
|------|------|
| [`docs/architecture.md`](./docs/architecture.md) | 헥사고날 구조, 도메인 모듈 목록 |
| [`docs/security.md`](./docs/security.md) | JWT 인증/토큰 흐름, 시크릿 관리 |
| [`docs/database.md`](./docs/database.md) | DB 프로파일, JPA 설정, 운영 규칙 |
| [`docs/db_schema.md`](./docs/db_schema.md) | 전체 테이블·컬럼 스키마 |
| [`docs/async-mail.md`](./docs/async-mail.md) | 비동기 처리 및 메일 발송 |
| [`docs/notes.md`](./docs/notes.md) | 개발 중 발견된 수정/개선 TODO |
