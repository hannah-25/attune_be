# 문서 최신성(Doc Gardening) 워크플로

문서가 낡지 않게 유지하는 절차. **코드 변경 시 같은 PR에서** 문서를 갱신한다.

## 코드 → 문서 매핑

| 코드 변경 | 갱신할 문서 |
|-----------|-------------|
| 도메인 모듈 추가/제거/이름변경 | `ARCHITECTURE.md` 모듈표, `docs/architecture/index.md`, `CLAUDE.md` 표, `docs/generated/project-map.md` |
| 엔티티/컬럼 추가·변경 | `docs/db_schema.md`, `docs/generated/data-schema.md` |
| API 엔드포인트 추가/변경/삭제 | `docs/api-guide.md`, `docs/generated/api-index.md` |
| 인증/토큰/시크릿 흐름 | `docs/security.md`, `docs/architecture/security-rules.md` |
| 프로파일/DB/JPA 설정 | `docs/database.md`, `docs/architecture/system-overview.md` |
| 비동기/메일 | `docs/async-mail.md` |
| 빌드/실행/테스트 명령 | `AGENTS.md`, `CLAUDE.md`, `scripts/agent/*` |
| 새 TODO/개선거리 | `docs/notes.md`, `docs/exec-plans/tech-debt-tracker.md` |

## 규칙

- 확실하지 않은 내용은 `ASSUMPTION:` 또는 `TODO(owner, YYYY-MM-DD, reason):` 로 표시한다.
- 낡은 문서는 바로 지우지 말고 상단에 `> DEPRECATED: 대체 → <링크>` 표시 후 한 사이클 뒤 제거.
- 자동 생성 문서(`docs/generated/`)는 손으로 고치지 않는다. 스크립트로만 갱신.
- 문서와 코드가 다르면 **코드를 정본**으로 보고 문서를 고친다.

## 정기 점검

```bash
scripts/agent/check-docs              # 깨진 링크 + generated 문서 신선도 점검
scripts/agent/generate-project-map    # 모듈 목록 재생성
scripts/agent/generate-api-index      # 엔드포인트 재생성
scripts/agent/generate-data-schema    # 엔티티 인벤토리 재생성
```

점검 결과 발견된 드리프트는 [tech-debt-tracker](../exec-plans/tech-debt-tracker.md) 에 등록한다.
