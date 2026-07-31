# 데이터베이스

테이블/컬럼 정의는 [`db_schema.md`](./db_schema.md) 참고.

## 운영

- 모든 런타임 프로파일(`local`/`dev`/`prod`)은 **MySQL 8.4**, 테스트는 **Testcontainers MySQL 8.4**(Docker 필요, `jdbc:tc:` URL로 자동 기동)를 사용한다.
- `local`의 datasource(URL/계정)는 `application-secret.yml`에서 주입된다.
- JPA DDL auto는 프로파일별로 설정된다 (local/dev: `update`, prod: `validate`).
- OSIV(`open-in-view`)는 **비활성화** — 서비스에서 트랜잭션 내에 필요한 모든 데이터를 조회해야 한다.
- `User` 엔티티의 기본 키는 **UUID** 문자열이다.
- 서버/JVM/DB 연결 timezone은 **Asia/Seoul**을 기본값으로 사용한다. (향후 글로벌 전환 시 UTC로 변경 예정)
- 사용자 화면/복약 알림처럼 현지 시간이 필요한 로직은 JVM 기본 timezone에 기대지 않고 `ZoneId`를 명시해 계산한다.

## 설정 프로파일

| 프로파일 | DB | 비고 |
|----------|-----|------|
| `local` | MySQL | 기본값; 디버그 로깅; localhost CORS |
| `dev` | MySQL | 원격 개발 서버; 디버그 로깅 |
| `prod` | MySQL | HTTPS 필수; Actuator 접근 제한 |

민감 정보는 `application-secret.yml`(gitignore 처리됨)을 통해 주입된다. `dev`/`prod` 실행 전 팀 공유 템플릿을 복사해 값을 채워야 한다.
