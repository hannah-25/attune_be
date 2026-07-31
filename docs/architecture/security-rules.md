# 보안 아키텍처 규칙

상세 흐름은 [`../security.md`](../security.md), 보안 체크리스트는 [`../quality/security.md`](../quality/security.md).

## 인증 / 인가

1. 인증은 **JWT (Bearer)** + Spring Security. 검증은 `common/filter/JwtAuthenticationFilter` 한 곳에서.
2. 사용자 식별은 Service 레이어에서 `SecurityUtils.getCurrentUserUuid()` 로. 컨트롤러에서 UUID를 신뢰해 받지 않는다.
3. 인가 정책(권한/역할, 어드민 분리)은 `SecurityConfig` 또는 메서드 보안으로 격리한다. 컨트롤러 본문에 권한 분기를 흩뿌리지 않는다.
4. 어드민 영역(`admin/**`)은 일반 사용자와 권한 경계를 분리한다.

## 시크릿 / 환경 변수

1. 모든 시크릿(JWT 키, VAPID 키, DB 비밀번호, Gemini 키, HMAC 시크릿)은 `application-secret.yml`(git 미추적) 또는 환경변수로 주입한다.
2. 시크릿을 **코드·문서·로그·테스트·커밋 메시지에 절대 쓰지 않는다.**
3. CI는 `secrets.APPLICATION_SECRET_YML` 를 base64로 복원해 빌드 시점에만 사용한다.
4. 비밀이 아닌 값(예: web-push subject)은 공통 기본값으로 둘 수 있다.

## 데이터 보호

1. 개인정보 저장은 최소화. 감사 로그(admin/audit)는 식별자를 **HMAC 해시**로 보관한다.
2. 로그에 토큰/비밀번호/개인정보를 남기지 않는다(마스킹).
3. 탈퇴/비활성 계정은 정책에 따라 소프트 삭제 후 영구 삭제 경로를 거친다(`user` 도메인).

## Management / Actuator

1. dev/prod의 Actuator는 public 앱 포트와 분리된 management server에 둔다.
2. management server는 기본 `127.0.0.1:8081`에 바인딩해 EC2 내부 배포 게이트만 접근하게 한다.
3. `/actuator/health/**`·`/actuator/info`만 허용하고, `/actuator/metrics` 등 나머지 actuator HTTP endpoint는 Spring Security에서 `denyAll`로 차단한다.
4. management address/port 변경은 배포 health gate와 함께 검토한다.

## 주요 위험 대응

| 위험 | 대응 |
|------|------|
| CORS | `CorsProperties` + SecurityConfig 화이트리스트 |
| CSRF | `SecurityConfig` 에서 비활성(확인됨). 인증은 `Authorization: Bearer` **헤더** 기반이라 쿠키 CSRF 벡터가 없음. 단 CORS `allowCredentials=true` 이므로 토큰을 쿠키로 옮기면 CSRF 재검토 필요 |
| XSS | 응답은 JSON, 입력 검증/이스케이프는 클라이언트+서버 양측 |
| SQL Injection | JPA 파라미터 바인딩 사용, 문자열 쿼리 연결 금지 |
| 토큰 탈취 | 만료/갱신 + Redis 캐시 무효화(`UserAuthCacheEvictor`) |

## 강제 수단

- 보안 변경은 PR 체크리스트의 인증/인가·시크릿 항목으로 점검.
- 시크릿 유출 방지: `scripts/agent/verify` 의 secret 스캔(soft) + `.gitignore` 의 `application-secret.yml`.
- CI에 secret 스캔 잡 추가는 [tech-debt-tracker](../exec-plans/tech-debt-tracker.md) 에 후보로 기록.
