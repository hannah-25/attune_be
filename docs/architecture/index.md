# Architecture 규칙 색인

시스템 구조의 개요는 루트 [`ARCHITECTURE.md`](../../ARCHITECTURE.md).
이 디렉터리는 **강제 가능한 규칙**을 주제별로 나눈다. 각 규칙은 가능하면 검증 수단(ArchUnit/테스트/CI)과 연결한다.

| 문서 | 주제 | 강제 수단 |
|------|------|-----------|
| [system-overview.md](./system-overview.md) | 런타임 구성요소, 요청 흐름 | (설명) |
| [module-rules.md](./module-rules.md) | 모듈 내부 레이아웃 규칙 | ArchUnit (soft) |
| [dependency-rules.md](./dependency-rules.md) | 계층/모듈 간 의존 방향 | ArchUnit |
| [data-rules.md](./data-rules.md) | 엔티티·트랜잭션·OSIV·DTO 분리 | ArchUnit (부분) + 리뷰 |
| [api-rules.md](./api-rules.md) | REST·DTO·상태코드·버저닝 | 리뷰 + springdoc |
| [security-rules.md](./security-rules.md) | 인증/인가/시크릿 | 리뷰 + 보안 체크리스트 |
| [error-handling-rules.md](./error-handling-rules.md) | 예외 계층·매핑 | ArchUnit (부분) + 리뷰 |

> **soft rule** = 지금 강제하면 기존 코드가 깨질 수 있어 경고/문서로만 두는 규칙.
> 강제로 승격하는 시점은 [tech-debt-tracker](../exec-plans/tech-debt-tracker.md) 에서 추적한다.
