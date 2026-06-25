# docs/ — Attune 하네스 지식 저장소

이 디렉터리는 AI 에이전트와 사람이 공유하는 **구조화된 지식**이다.
빠른 진입점은 루트 [`AGENTS.md`](../AGENTS.md), 구조는 [`ARCHITECTURE.md`](../ARCHITECTURE.md).

## 디렉터리 지도

| 경로 | 내용 |
|------|------|
| [`product/`](./product/index.md) | 제품 목적·사용자·도메인 개요 |
| [`architecture/`](./architecture/index.md) | 계층/의존성/데이터/API/보안/예외 **규칙** |
| [`engineering/`](./engineering/coding-conventions.md) | 코딩 컨벤션, 테스트 전략, CI/CD, 배포, 관측성 |
| [`agent/`](./agent/agent-workflow.md) | 에이전트 작업 루프 + 작업 유형별 워크플로 + 리뷰 체크리스트 |
| [`exec-plans/`](./exec-plans/template.md) | 복잡한 작업의 실행 계획 + 기술부채 트래커 |
| [`generated/`](./generated/) | **자동 생성** 산출물 (수동 수정 금지) |
| [`quality/`](./quality/quality-score.md) | 품질 점수·신뢰성·보안·유지보수성 |

## 기존 레퍼런스 문서 (도메인 상세)

하네스 도입 이전부터 있던 상세 문서. 그대로 유효하다:

| 문서 | 내용 |
|------|------|
| [`api-guide.md`](./api-guide.md) | Journal API 엔드포인트 명세 |
| [`db_schema.md`](./db_schema.md) | 전체 테이블·컬럼 스키마 (**새 엔티티 전 필독**) |
| [`security.md`](./security.md) | JWT 인증/토큰 흐름, 시크릿 관리 |
| [`database.md`](./database.md) | DB 프로파일, JPA 설정, 운영 규칙 |
| [`async-mail.md`](./async-mail.md) | 비동기 처리 및 메일 발송 |
| [`notes.md`](./notes.md) | 개발 중 발견된 수정/개선 TODO |

### 기능별 설계/스펙/감사 기록

과거에 작성된 기능 단위 문서. 이력 참고용으로 유효하다(최신 정본은 코드 + `generated/`):

| 문서 | 내용 |
|------|------|
| [`medication-analysis-report-spec.md`](./medication-analysis-report-spec.md) | 약물 치료 경과 리포트 기획 명세 |
| [`medication-analysis-report-plan.md`](./medication-analysis-report-plan.md) | 약물 치료 경과 리포트 구현 계획 |
| [`medication_consultation_model.md`](./medication_consultation_model.md) | Medication–Consultation 데이터 모델 |
| [`medication_api_entity_audit.md`](./medication_api_entity_audit.md) | Medication API↔Entity 구조 검토 |
| [`user_medications_api_spec.md`](./user_medications_api_spec.md) | User Medications API 스펙 |
| [`token-refresh-api-spec.md`](./token-refresh-api-spec.md) | Token Refresh API 스펙 (프론트) |
| 그 외 `*-followup.md`, `journal-tag-*.md` | 후속작업/마이그레이션 기록 |

> 새 설계·계획 문서는 [`exec-plans/`](./exec-plans/template.md) 체계를 사용한다. 위 문서들은 점진적으로 그쪽으로 이관하거나 코드 정본으로 흡수한다.

## 문서 규칙

- 코드를 바꾸면 **같은 PR에서** 영향받는 문서를 갱신한다 → [doc-gardening-workflow](./agent/doc-gardening-workflow.md).
- 확실하지 않은 내용은 `ASSUMPTION:` 또는 `TODO(owner, date, reason):` 로 표시한다.
- 자동 생성 문서(`generated/`)는 상단의 "수동 수정 금지" 표시를 지키고, 스크립트로만 갱신한다.
- 문서와 코드가 다르면 **코드를 정본으로 보고** 문서를 고친다.
