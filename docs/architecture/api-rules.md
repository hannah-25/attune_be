# API 규칙

## REST 컨벤션

1. 컨트롤러는 `adapter/web`, `@RestController` + `@RequestMapping`.
2. 요청/응답은 DTO로 주고받는다. 엔티티 직접 노출 금지.
3. 검증은 요청 DTO의 Bean Validation(`@Valid`, `@NotNull` 등)으로.
4. 상태코드는 의미에 맞게: 생성 201, 성공 200/204, 클라이언트 오류 4xx, 서버 오류 5xx.
5. 오류 응답 본문은 `common/error/ErrorResponse` 형식으로 통일(`GlobalExceptionHandler` 경유).

## 인증 표시

- 인증이 필요한 엔드포인트는 SecurityConfig 규칙으로 보호된다. 공개 엔드포인트는 명시적으로 permitAll.
- 엔드포인트별 인증 필요 여부는 [security-rules.md](./security-rules.md) 와 [`../security.md`](../security.md) 기준.

## 문서화 (springdoc)

- 모든 공개 API는 Swagger에 노출된다(`/swagger-ui.html`, `/v3/api-docs`).
- 신규/변경 엔드포인트는 `@Operation`/`@Schema` 로 설명을 보강하고, [`../api-guide.md`](../api-guide.md) 와 [`../generated/api-index.md`](../generated/api-index.md) 를 갱신한다.

## 호환성

- 공개 API의 비호환 변경(필드 제거/타입 변경/상태코드 변경/인증 요구 변경)은 **사람 확인 필요**.
- 추가는 하위호환을 유지하는 방향(옵셔널 필드 추가 등)으로.

## 강제 수단

- `scripts/agent/generate-api-index` 가 컨트롤러를 스캔해 [`../generated/api-index.md`](../generated/api-index.md) 를 만든다 → 문서 드리프트 점검.
- API 변경 시 문서 갱신은 PR 체크리스트 항목.
