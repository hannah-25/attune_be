# CLAUDE.md

Spring Boot 4 / Java 17 기반의 헥사고날 아키텍처 백엔드 프로젝트.

이 프로젝트의 AI 에이전트 규칙·지식은 **하네스 시스템**으로 일원화돼 있다.
CLAUDE.md는 별도 규칙을 두지 않고, 아래 진입점을 그대로 따른다.

## 작업 시작 전 반드시

1. **[`AGENTS.md`](./AGENTS.md)** — 단일 진입점. 작업 전 먼저 읽는다.
2. [`docs/agent/agent-workflow.md`](./docs/agent/agent-workflow.md) — 표준 작업 루프
3. [`docs/README.md`](./docs/README.md) — 구조화된 지식 전체 색인

명령어는 [`scripts/agent/`](./scripts/agent/README.md), 아키텍처·도메인 모듈은 [`ARCHITECTURE.md`](./ARCHITECTURE.md) 참고.

> AGENTS.md의 규칙(보호 브랜치 직접 push 금지, secret 미커밋, db_schema 우선 확인,
> 작업 후 문서 갱신 등)은 CLAUDE.md에도 동일하게 적용된다.
