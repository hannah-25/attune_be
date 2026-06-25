# 기능 추가 워크플로

1. **요구 확정** — 입력/출력/엔드포인트/권한/데이터를 명확히 한다. 모호하면 사람 확인.
2. **데이터 확인** — 새 엔티티/컬럼이 필요하면 [`../db_schema.md`](../db_schema.md) **먼저** 확인 → 없거나 다르면 사람 확인.
3. **모듈 선택/생성** — 기존 도메인에 속하면 그 모듈, 아니면 [module-rules](../architecture/module-rules.md) 형태로 신규 모듈.
4. **레이어대로 구현**:
   - `domain/model` 엔티티 → `domain/repository` → `application` 서비스(+DTO) → `adapter/web` 컨트롤러.
   - 외부 연동은 `adapter/<vendor>`·`infrastructure`.
5. **인증/인가** — 새 엔드포인트의 보호 여부를 SecurityConfig 기준으로 명시 ([security-rules](../architecture/security-rules.md)).
6. **테스트** — 서비스 단위 + 컨트롤러/보안 테스트. 시간·외부 의존은 고정/목.
7. **문서** — `api-guide.md`/`generated/api-index.md`, 엔티티면 `db_schema.md`/`generated/data-schema.md`.
8. **검증** — `scripts/agent/verify`.
9. **요약** — [task-template](./task-template.md) + [pr-review-checklist](./pr-review-checklist.md).

## 흔한 실수 (이 리포 특화)

- OSIV 꺼짐 → 컨트롤러/DTO 매핑 단계 지연 로딩 금지. 서비스에서 fetch.
- 엔티티를 응답으로 직접 노출 → 반드시 응답 DTO.
- 현재 사용자 UUID를 요청에서 받기 → `SecurityUtils.getCurrentUserUuid()` 사용.
- 단일이 아닌 생성자에 `@Autowired` 누락 → 기동 실패. 명시.
