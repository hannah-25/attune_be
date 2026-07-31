# 리팩터링 워크플로

원칙: **동작을 바꾸지 않는다.** 동작 변경이 섞이면 별도 작업/커밋으로 분리한다.

1. **안전망 확인** — 대상 코드에 테스트가 있는지 확인. 없으면 **특성화 테스트 먼저** 추가.
2. **범위 한정** — 한 번에 하나의 관심사. 거대 일괄 변경 금지(특히 일괄 포맷/리네임).
3. **규칙 방향으로** — [module-rules](../architecture/module-rules.md)/[dependency-rules](../architecture/dependency-rules.md) 를 더 잘 만족하는 방향으로.
4. **단계별 검증** — 각 단계 후 `scripts/agent/test`. green 유지하며 진행.
5. **아키텍처 검증** — `scripts/agent/verify` 의 ArchUnit 통과(드리프트 감소 확인).
6. **문서** — 구조/모듈/규칙이 바뀌면 `ARCHITECTURE.md`·`docs/architecture/*` 갱신.

## 주의

- 리팩터링으로 의존 사이클을 만들지 않는다.
- soft rule 위반을 줄이면 [tech-debt-tracker](../exec-plans/tech-debt-tracker.md) 에서 해당 항목을 닫는다.
