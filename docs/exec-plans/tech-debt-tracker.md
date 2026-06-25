# 기술 부채 트래커

하네스가 발견했지만 이번에 강제하지 않은 항목, soft rule, 드리프트. 각 항목은 사유와 다음 행동을 남긴다.

| ID | 항목 | 종류 | 상태 | 다음 행동 |
|----|------|------|------|-----------|
| TD-1 | 배포 빌드가 `-x test` 로 테스트 스킵 | CI | 완화됨 | `ci.yml` 이 PR에서 테스트 강제. develop required check 설정 필요(사람) |
| TD-2 | `docs/architecture.md`(구버전) 모듈 목록/레이아웃 낡음 | 문서 드리프트 | closed(2026-06-25) | `ARCHITECTURE.md` 로 통합·삭제. 참조(CLAUDE.md) 갱신 완료 |
| TD-3 | Spotless 포맷 검사 미강제(non-blocking) | 코드 스타일 | open | 변경 파일 범위부터 정리 → 안정화 후 `check` 에 강제 승격 |
| TD-4 | ArchUnit 일부 규칙 soft(컨트롤러→Repository 직접호출, 슬라이스 순환) | 아키텍처 | open | 위반 0 확인되면 강제 승격 |
| TD-5 | DB 스키마 마이그레이션 도구(Flyway/Liquibase) 미사용 | 데이터 | open | 도입 검토. 현재 JPA ddl 기반 |
| TD-6 | 테스트 커버리지 측정(JaCoCo) 없음 | 테스트 | open | JaCoCo 도입 + CI 리포트 |
| TD-7 | CI에 secret 스캔 잡 없음 | 보안 | open | gitleaks 등 도입 검토 |
| TD-8 | GitHub 브랜치 보호(required check `ci.yml`) 미설정 | CI | open | maintainer가 설정 |
| TD-9 | `docs/medication_api_entity_audit.md` 에 머신 종속 절대경로 링크(`/D:/...`) 다수 | 문서 | closed(2026-06-25) | 상대경로(`../src/...`)로 교체 완료. check-docs 통과 |

## 규칙

- 새 부채 발견 시 행을 추가하고 ID를 부여한다.
- 해소되면 상태를 `closed` 로 바꾸고(행 유지) 날짜를 남긴다.
- soft rule을 강제(blocking)로 승격하면 관련 ArchUnit/CI 설정도 함께 바꾼다.
