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

ASSUMPTION: 대부분 엔드포인트는 인증 필요, 인증/공개(약관·회원가입·헬스)만 permitAll.
정확한 매트릭스는 SecurityConfig 기준이며, `generated/api-index.md` 와 대조해 유지한다.
`TODO(security-owner, 2026-06-25, 엔드포인트별 인증 매트릭스 표 확정)`.

## 자동화 후보

- CI에 secret 스캔(gitleaks) 잡 추가 — [tech-debt-tracker](../exec-plans/tech-debt-tracker.md) TD-7.
- 의존성 취약점 스캔(OWASP dependency-check / Dependabot).
