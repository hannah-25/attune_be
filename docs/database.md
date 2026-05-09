# 데이터베이스

테이블/컬럼 정의는 [`db_schema.md`](./db_schema.md) 참고.

## 운영

- `dev`/`prod` 프로파일은 **MySQL 8.4**, `local` 및 테스트는 **H2 인메모리** DB를 사용한다.
- JPA DDL auto는 프로파일별로 설정된다 (dev: `update`, prod: `validate`).
- OSIV(`open-in-view`)는 **비활성화** — 서비스에서 트랜잭션 내에 필요한 모든 데이터를 조회해야 한다.
- `User` 엔티티의 기본 키는 **UUID** 문자열이다.

## 설정 프로파일

| 프로파일 | DB | 비고 |
|----------|-----|------|
| `local` | H2 | 기본값; 디버그 로깅; localhost CORS |
| `dev` | MySQL | 원격 개발 서버; 디버그 로깅 |
| `prod` | MySQL | HTTPS 필수; Actuator 접근 제한 |

민감 정보는 `application-secret.yml`(gitignore 처리됨)을 통해 주입된다. `dev`/`prod` 실행 전 팀 공유 템플릿을 복사해 값을 채워야 한다.
