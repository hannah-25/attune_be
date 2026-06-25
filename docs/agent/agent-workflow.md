# 에이전트 작업 루프

모든 작업의 기본 루프. 작업 유형별 세부는 아래 링크 참고.

## 표준 루프

1. **읽기** — [`AGENTS.md`](../../AGENTS.md) 와 이 문서를 읽는다.
2. **관련 문서 찾기** — 작업 주제에 맞는 `docs/` 문서를 연다(architecture/, engineering/, 도메인 레퍼런스).
3. **현재 코드 확인** — 대상 모듈 구조와 기존 패턴을 파악한다(추측 금지).
4. **계획 작성** — 변경 범위/제외/위험을 정한다. 복잡하면 [`exec-plans/template.md`](../exec-plans/template.md) 로 `active/` 에 계획 파일 생성.
5. **작은 단위 구현** — 한 번에 한 관심사. 레이아웃·의존성 규칙 준수.
6. **테스트 실행** — `scripts/agent/test` 또는 해당 단위 테스트.
7. **실패 분석/수정** — [observability](../engineering/observability.md) 로 원인 좁히기. 통과까지 반복.
8. **문서 갱신** — [doc-gardening-workflow](./doc-gardening-workflow.md) 의 매핑대로 영향 문서 갱신.
9. **규칙 검증** — `scripts/agent/verify` 로 빌드+테스트+아키텍처 규칙 확인.
10. **변경 요약/PR** — [task-template](./task-template.md) 형식으로 요약, [pr-review-checklist](./pr-review-checklist.md) 통과.

## 작업 유형별 워크플로

| 유형 | 문서 |
|------|------|
| 기능 추가 | [feature-workflow.md](./feature-workflow.md) |
| 버그 수정 | [bugfix-workflow.md](./bugfix-workflow.md) |
| 리팩터링 | [refactor-workflow.md](./refactor-workflow.md) |
| 문서 정리 | [doc-gardening-workflow.md](./doc-gardening-workflow.md) |
| 테스트 추가 | [testing-strategy](../engineering/testing-strategy.md) |
| 보안 수정 | [security-rules](../architecture/security-rules.md) + [quality/security](../quality/security.md) |
| 배포 수정 | [deployment-rules](../engineering/deployment-rules.md) |
| 성능 개선 | [quality/reliability](../quality/reliability.md) |

## 멈추지 않는 원칙

- 계획한 작업이 끝날 때까지(테스트 green + 문서 갱신 + 규칙 통과) 진행한다.
- 막히면 [확인 기준](../../AGENTS.md#8-확실하지-않을-때-사람에게-확인할-기준)에 해당하는지 보고, 해당하면 사람에게 확인한다.
