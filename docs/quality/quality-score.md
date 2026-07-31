# 품질 점수 (스냅샷)

하네스 도입 시점(2026-06-25)의 자가 평가. 절대값이 아니라 추세 추적용이다. 갱신 시 날짜와 함께 한 행 추가.

| 영역 | 점수(1-5) | 근거 | 개선 포인터 |
|------|-----------|------|-------------|
| 빌드/실행 | 4 | Gradle 표준, 프로파일 분리, 로컬 H2 | — |
| 테스트 | 4 | Testcontainers MySQL/Redis 기반 HTTP→DB 통합 테스트 41개 추가. auth~기타 도메인 핵심 시나리오 로컬 전체 테스트 통과. 커버리지 미측정과 CI 연속 안정성 확인은 남음 | [testing-strategy](../engineering/testing-strategy.md), TD-6 |
| 아키텍처 일관성 | 4 | 헥사고날 레이아웃 일관. 검증 도구 신규 도입 | ArchUnit 강제 승격(TD-4) |
| 문서화 | 3→4 | 레퍼런스 문서 풍부했으나 평면적 → 하네스로 구조화 | generated 자동화 유지 |
| CI/CD | 2→3 | 배포 자동화는 양호, PR 검증 게이트 부재 → `ci.yml` 추가 | required check 설정(TD-8) |
| 보안 | 3 | 시크릿 분리·감사 해시 양호. secret 스캔 미자동화 | TD-7 |
| 관측성 | 3 | actuator/health, 로그 패턴. health의 db indicator off | [observability](../engineering/observability.md) |

세부 영역: [reliability.md](./reliability.md) · [security.md](./security.md) · [maintainability.md](./maintainability.md).
