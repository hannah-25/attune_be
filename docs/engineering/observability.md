# 관측성 / 디버깅

AI 에이전트가 문제를 빠르게 좁히기 위한 진입점.

## Health / 상태

- Health(aggregate): dev/prod는 `GET http://127.0.0.1:8081/actuator/health`, local은 `GET /actuator/health`. `db`·`diskspace` 외에 Redis 등 모든 가용 indicator가 합산된다 → 일부 의존성 장애 시 503. **배포 게이트는 이 aggregate를 쓰지 않는다**(아래 readiness 참조).
- Liveness: dev/prod는 `GET http://127.0.0.1:8081/actuator/health/liveness` — JVM 생존만 검사. 외부 의존성 장애로 불필요하게 실패하지 않는다.
- Readiness: dev/prod는 `GET http://127.0.0.1:8081/actuator/health/readiness` — `readinessState + db`. **배포 게이트(`deploy-prod`/`deploy-dev`)가 EC2 내부 loopback endpoint를 폴링**한다. DB 장애는 실패로 반영하되 Redis/메일에는 묶이지 않는다.
- 보안: dev/prod의 actuator는 public 앱 포트(8080)와 분리된 management server `127.0.0.1:8081`에 바인딩한다. `/actuator/health/**`·`/actuator/info`만 허용하고 `/actuator/metrics` 등 나머지는 Spring Security에서 `denyAll`로 차단한다(메트릭은 in-process Micrometer로만 수집). 메트릭 태그 `application=attune`.
- dev 서버 부하 테스트 시에는 `SPRING_PROFILES_ACTIVE=dev,loadtest`를 사용한다. health/probe는 base에서 켜지며, 이 프로파일은 로그 레벨을 낮추고 메트릭에 `environment=loadtest` 태그를 붙이며 web-push를 stub으로 바꾼다.

## 로컬 로그

- `./gradlew bootRun` 콘솔 로그. 패턴: `HH:mm:ss.SSS [thread] LEVEL [requestId] logger - msg`.
- 모든 HTTP 요청은 `X-Request-Id`를 응답 헤더로 반환한다. 클라이언트가 안전한 `X-Request-Id`를 보내면 그대로 사용하고, 없거나 안전하지 않으면 서버가 UUID를 생성한다.
- 애플리케이션 로그는 SLF4J MDC의 `requestId`를 포함한다. 요청 밖에서 발생한 로그는 `no-request-id`로 표시된다.
- `@Async` executor는 MDC를 전달하므로, 요청 중 시작된 비동기 작업 로그도 같은 `requestId`로 검색할 수 있다.
- `attune.calendar` 는 기본 DEBUG. 특정 모듈 디버깅은 `logging.level.attune.<module>: DEBUG` 추가.
- 백그라운드 실행 로그: `bootRun.out.log` / `bootRun.err.log`(루트).
- **민감정보 로깅 금지**: 이메일 주소·JWT·비밀번호·인증코드, 증상/일기/복약/AI 프롬프트·응답 본문은 로그에 남기지 않는다. 외부 API response body와 push title/body도 사용자 콘텐츠를 포함할 수 있으므로 로그와 예외 cause에 보존하지 않는다. 식별이 필요하면 `userId`(UUID) 같은 내부 식별자만 사용한다. 예: 발송 실패 로그는 `email` 대신 `userId={}`로 남긴다.

## 테스트 실패 로그 읽기

- `./gradlew test` 실패 시 `build/reports/tests/test/index.html` 에 리포트.
- 단일 재현: `./gradlew test --tests "attune.Xxx.method" --info`.

## 연결 확인

| 대상 | 확인법 |
|------|--------|
| DB(MySQL/H2) | 부팅 로그의 Hikari/Hibernate, 쿼리 로그. local은 H2 콘솔(설정 시) |
| Redis | 토큰 캐시 동작(로그인/갱신/로그아웃). 연결 실패 시 부팅 로그 |
| Mail(SMTP) | 메일 발송 비동기 로그, [`../async-mail.md`](../async-mail.md) |
| web-push | `provider` 가 `stub`(local) vs `web-push`(dev/prod). 발송 이력 |
| Gemini | `ai`/`medicationAnalysis` 클라이언트 응답 검증기 로그 |

## 부하 테스트

- 기본 대상은 dev 서버이며, 실행 프로파일은 `dev,loadtest`를 권장한다.
- 부하 테스트 중에는 회원가입/메일 발송, 실제 푸시 발송, Gemini 호출을 기본 시나리오에 넣지 않는다. Gemini는 비용과 외부 API rate limit 영향을 분리하기 위해 별도 테스트로 측정한다.
- 우선 측정할 API는 인증된 조회/기록 흐름이다: 로그인 토큰 준비 → 오늘 조회 → 복약 기록 → 일정 조회 → 일지/태그 조회.
- 결과에는 동시 사용자 수, duration, p95/p99, 5xx 비율, Hikari active/pending, JVM heap/GC를 함께 기록한다.

## 인증/API 실패 디버깅

- 401: 토큰 만료/서명/필터(`JwtAuthenticationFilter`) 확인. `SecurityErrorResponseWriter` 응답.
- 403: 인가/권한 경계(SecurityConfig, 어드민 분리).
- 4xx 본문: `GlobalExceptionHandler` → `ErrorResponse` 의 메시지/필드로 원인 파악.

## 배포 실패 디버깅

- 워크플로 로그 → EC2 `docker logs --tail 300 attune-dev-apps`.
- health 600s 타임아웃이면 컨테이너 exitCode/State 확인(워크플로가 자동 출력).
