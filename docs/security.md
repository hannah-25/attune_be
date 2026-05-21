# 보안 및 토큰 흐름

- **Stateless JWT** — 서버 측 세션 없음.
- `JwtAuthenticationFilter`가 컨트롤러 도달 전 매 요청마다 액세스 토큰을 검증한다.
- 액세스 토큰은 `Authorization: Bearer …` 헤더로 전달되고, 리프레시 토큰은 **HttpOnly 쿠키**에 저장된다.
- 두 토큰 모두 TTL을 설정해 **Redis**에 보관한다 (`UserAuthCache` 참고).
- JWT 시크릿, DB 자격증명 등 민감 정보는 `application-secret.yml`에 관리하며, 이 파일은 git에서 제외된다. 로컬 환경 구성 시 팀 공유 템플릿을 복사해 실제 값을 채워야 한다.
