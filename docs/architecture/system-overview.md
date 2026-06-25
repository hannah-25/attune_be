# 시스템 개요

## 런타임 구성요소

```
[Client] ──HTTPS──> [Spring Boot App (attune)]
                        ├─ Web MVC (REST, springdoc/Swagger)
                        ├─ Spring Security + JwtAuthenticationFilter
                        ├─ JPA/Hibernate ──> [MySQL]   (dev/prod) / [H2] (local·test)
                        ├─ Redis ──> 토큰 캐시(UserAuthCache)
                        ├─ Caffeine ──> 로컬 인메모리 캐시
                        ├─ Mail (SMTP/Gmail) ──> 이메일 발송
                        ├─ web-push (VAPID) ──> 푸시 알림
                        └─ Gemini client ──> AI 리포트/추천
```

배포: GitHub Actions → DockerHub(`hannah098/attune-be`) → EC2에서 `docker run`(host network). Health는 `/actuator/health`.

## 요청 처리 흐름 (인증 경로)

1. 클라이언트가 `Authorization: Bearer <JWT>` 로 요청.
2. `JwtAuthenticationFilter`(`common/filter`)가 토큰 검증 → `SecurityContext` 채움.
3. 컨트롤러(`adapter/web`)가 요청 DTO 바인딩 후 Service 호출.
4. Service(`application`)가 `@Transactional` 경계에서 유스케이스 수행, `SecurityUtils.getCurrentUserUuid()` 로 사용자 식별.
5. Repository(`domain/repository`)로 데이터 접근, 엔티티(`domain/model`) 조작.
6. 응답 DTO로 변환해 반환. 예외는 `GlobalExceptionHandler`가 HTTP 상태로 매핑.

## 프로파일

| 프로파일 | DB | 용도 |
|----------|----|------|
| `local`(기본) | H2 인메모리 | 로컬 개발 |
| `dev` | MySQL | 개발 서버(EC2) |
| `prod` | MySQL | 운영 |
| (test) | H2 | 테스트, `Asia/Seoul` 타임존 고정 |

시크릿은 `application-secret.yml`(미추적)에서 프로파일별 문서로 주입. 자세한 내용 [`../database.md`](../database.md), [`../security.md`](../security.md).
