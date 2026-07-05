# AGENTS.md — Attune 백엔드 작업 지도

> 이 문서는 **지도**다. 모든 규칙을 담지 않고, 어디를 봐야 하는지만 알려준다.
> 상세 지식은 [`docs/`](./docs/README.md) 아래에 구조화되어 있다.
> AI 에이전트는 작업을 시작하기 전에 이 문서와 [작업 루프](./docs/agent/agent-workflow.md)를 먼저 읽는다.

## 1. 프로젝트 한 줄 요약

Attune 백엔드 — **성인 ADHD 사용자**를 위한 앱의 Spring Boot API 서버. **복약 관리와 상태 기록**이 핵심이고,
일정·진료·알림·AI 리포트가 이를 보조한다. 제품 관점은 [`docs/product/index.md`](./docs/product/index.md), 도메인 상세는 [`docs/product/domain-overview.md`](./docs/product/domain-overview.md).

## 2. 기술 스택 요약

| 영역 | 사용 기술 |
|------|-----------|
| 언어/런타임 | Java 17 |
| 프레임워크 | Spring Boot 4.0.2 (Web MVC, Data JPA, Security, Cache, Mail, Actuator) |
| 빌드 | Gradle (`./gradlew`) |
| DB | MySQL(local/dev/prod), 테스트는 Testcontainers MySQL(Docker 필요) |
| 캐시/세션 | Redis (토큰 캐시), Caffeine (로컬 캐시) |
| 인증 | JWT (jjwt) + Spring Security, 소셜 OAuth |
| 푸시 | web-push (VAPID) |
| AI | Google Gemini |
| API 문서 | springdoc-openapi (Swagger UI) |
| CI/CD | GitHub Actions → DockerHub → EC2(Docker) |

## 3. 주요 디렉터리

```
src/main/java/attune/        # 도메인 모듈 (헥사고날)
  <domain>/adapter/web/      # REST 컨트롤러 (인바운드 어댑터)
  <domain>/application/      # 서비스 + DTO (유스케이스)
  <domain>/domain/model/     # JPA 엔티티, 열거형
  <domain>/domain/repository # Spring Data JPA (아웃바운드 포트)
  common/                    # Security/Jwt/예외/공통 설정·유틸
src/main/resources/          # application*.yml, prompts/
src/test/java/attune/        # 테스트 (도메인 구조 미러링)
docs/                        # 하네스 지식 저장소 (→ docs/README.md)
scripts/agent/               # 에이전트용 표준 명령어
.github/workflows/           # CI(ci.yml) + 배포(deploy-*.yml)
```

모듈 목록과 책임은 [`ARCHITECTURE.md`](./ARCHITECTURE.md) 참고.

## 4. 자주 쓰는 명령어

> 표준 진입점은 [`scripts/agent/`](./scripts/agent/) 이다. Windows에서는 Git Bash 또는 `bash scripts/agent/<name>` 으로 실행한다.

| 목적 | 스크립트 | 원시 명령 |
|------|----------|-----------|
| 로컬 실행 (MySQL) | `scripts/agent/run-local` | `./gradlew bootRun` |
| 테스트 | `scripts/agent/test` | `./gradlew test` |
| 단일 테스트 | — | `./gradlew test --tests "attune.SomeTest"` |
| 빌드 | `scripts/agent/build` | `./gradlew clean build` |
| 전체 검증(빌드+테스트+아키텍처) | `scripts/agent/verify` | `./gradlew build` |
| 문서 점검 | `scripts/agent/check-docs` | — |
| 프로젝트 지도 생성 | `scripts/agent/generate-project-map` | — |

로컬 실행 후 Swagger UI: `http://localhost:8080/swagger-ui.html`, Health: `http://localhost:8080/actuator/health`.

## 5. 작업 전 반드시 확인할 문서

1. [`docs/agent/agent-workflow.md`](./docs/agent/agent-workflow.md) — 표준 작업 루프
2. [작업 유형별 워크플로](./docs/agent/) — feature / bugfix / refactor / doc-gardening
3. [`docs/architecture/`](./docs/architecture/index.md) — 계층·의존성·데이터·API·보안·예외 규칙
4. 새 엔티티/테이블 작업 시 → [`docs/db_schema.md`](./docs/db_schema.md) **먼저** 확인
5. 복잡한 작업이면 → [`docs/exec-plans/template.md`](./docs/exec-plans/template.md) 로 실행 계획 작성

## 6. PR 전 체크리스트 (요약)

전체 항목은 [`docs/agent/pr-review-checklist.md`](./docs/agent/pr-review-checklist.md).

- [ ] `scripts/agent/verify` 통과 (빌드 + 테스트 + 아키텍처 규칙)
- [ ] 변경한 코드에 대응하는 테스트 추가/갱신
- [ ] 영향받는 문서 갱신 ([문서 매핑](./docs/agent/doc-gardening-workflow.md))
- [ ] 계층 규칙 위반 없음 (adapter → application → domain)
- [ ] 새 엔티티/컬럼은 `docs/db_schema.md` 와 일치
- [ ] secret/token/운영 정보 미커밋

## 7. 에이전트가 하면 안 되는 것

- `develop` 등 보호 브랜치에 **직접 push 금지**. 항상 feature 브랜치 → PR → merge.
- `application-secret.yml`, VAPID 키, JWT 시크릿, 운영 DB 정보 등 **secret을 문서·코드·로그에 쓰지 않는다**.
- 운영 DB·운영 환경에 영향을 주는 명령(배포 워크플로, 운영 SSH 등)을 임의 실행하지 않는다.
- 기존 코드를 무리하게 일괄 포매팅/리네임하지 않는다 (불필요한 거대 diff 금지).
- 자동 생성 문서([`docs/generated/`](./docs/generated/))를 손으로 수정하지 않는다.

## 8. 확실하지 않을 때 사람에게 확인할 기준

다음 중 하나라도 해당하면 **진행 전 사람에게 확인**한다:

- `docs/db_schema.md` 에 없거나 다른 새 테이블/컬럼/제약 변경
- 공개 API의 비호환 변경(요청/응답 스키마, 상태코드, 인증 요구)
- 인증/인가/토큰 흐름 변경, 권한 경계 변경
- 데이터 마이그레이션·대량 삭제·되돌리기 어려운 작업
- 외부로 나가는 작업(메일·푸시 대량 발송, 외부 API 호출 비용 발생)
- 아키텍처 원칙과 충돌하는 설계 결정

---

문서 전체 색인은 [`docs/README.md`](./docs/README.md) 에 있다.
