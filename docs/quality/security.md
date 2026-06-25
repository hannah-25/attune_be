# 보안 체크리스트 (품질 게이트)

규칙 본문은 [architecture/security-rules.md](../architecture/security-rules.md), 흐름은 [`../security.md`](../security.md).

## 변경 시 체크

- [ ] 새 엔드포인트의 인증 필요 여부를 SecurityConfig에 명시했는가
- [ ] 인가/권한 경계(어드민 vs 일반)를 분리했는가
- [ ] 사용자 식별을 `SecurityUtils.getCurrentUserUuid()` 로 했는가(요청 입력 신뢰 금지)
- [ ] 입력 검증(Bean Validation)을 적용했는가
- [ ] secret/token/비밀번호/개인정보가 코드·로그·테스트·커밋에 없는가
- [ ] 예외/응답에 내부 정보가 새지 않는가
- [ ] 개인정보 저장을 최소화했는가, 식별자는 필요 시 해시(HMAC)인가
- [ ] 탈퇴/비활성/차단 계정 처리 경로에 영향이 없는가
- [ ] 파일 업로드가 있다면 타입/크기/저장 위치를 검증하는가
- [ ] SQL은 JPA 파라미터 바인딩인가(문자열 연결 금지)

## API별 인증 매트릭스

정본은 `common/config/SecurityConfig` 다. 아래는 그 규칙을 추출한 것(2026-06-25 기준).
규칙이 바뀌면 이 표와 함께 갱신한다.

| 분류 | 패턴 | 접근 |
|------|------|------|
| Preflight | `OPTIONS /**` | permitAll |
| 모니터링 | `/actuator/**`, `/v1/health/**` | permitAll |
| 인증/소셜 | `/auth/**`, `/oauth2/**`, `/login/oauth2/**`, `/v1/auth/login`·`/reissue`·`/restore`·`/social/login`·`/social/restore` | permitAll |
| 가입/이메일 | `/v1/account/signup`, `/v1/account/verify-email` | permitAll |
| 비밀번호 재설정 | `/v1/account/password/reset/**` | permitAll |
| 약관 조회 | `/v1/terms/**` | permitAll |
| 공지 조회 | `GET /v1/notices/**` | permitAll |
| API 문서 | `/swagger-ui/**`, `/v3/api-docs/**`, `/swagger-ui.html` | permitAll |
| 어드민 | `/v1/admin/**` | `ROLE_ADMIN` |
| 그 외 전부 | `anyRequest()` | 인증 필요 |

> 세션: `STATELESS`. 미인증 → 401("로그인이 필요합니다"), 권한 부족 → 403("관리자 권한이 필요합니다") — `SecurityErrorResponseWriter`.
> 신규 엔드포인트는 기본이 "인증 필요"다. 공개로 열려면 SecurityConfig에 **명시적으로** permitAll 추가 + 이 표 갱신.

## 자동화 후보

- CI에 secret 스캔(gitleaks) 잡 추가 — [tech-debt-tracker](../exec-plans/tech-debt-tracker.md) TD-7.
- 의존성 취약점 스캔(OWASP dependency-check / Dependabot).
