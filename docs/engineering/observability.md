# 관측성 / 디버깅

AI 에이전트가 문제를 빠르게 좁히기 위한 진입점.

## Health / 상태

- Health: `GET /actuator/health` (배포 게이트가 이걸 폴링). 노출 엔드포인트: `health, info, metrics`.
- Metrics: `GET /actuator/metrics`. 태그 `application=attune`.
- 주의: `application.yml` 에서 health 의 `db`·`diskspace` 인디케이터는 꺼져 있다 → DB 다운이 health 200을 막지 않는다. DB 연결은 별도 확인.

## 로컬 로그

- `./gradlew bootRun` 콘솔 로그. 패턴: `HH:mm:ss.SSS [thread] LEVEL logger - msg`.
- `attune.calendar` 는 기본 DEBUG. 특정 모듈 디버깅은 `logging.level.attune.<module>: DEBUG` 추가.
- 백그라운드 실행 로그: `bootRun.out.log` / `bootRun.err.log`(루트).

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

## 인증/API 실패 디버깅

- 401: 토큰 만료/서명/필터(`JwtAuthenticationFilter`) 확인. `SecurityErrorResponseWriter` 응답.
- 403: 인가/권한 경계(SecurityConfig, 어드민 분리).
- 4xx 본문: `GlobalExceptionHandler` → `ErrorResponse` 의 메시지/필드로 원인 파악.

## 배포 실패 디버깅

- 워크플로 로그 → EC2 `docker logs --tail 300 attune-dev-apps`.
- health 600s 타임아웃이면 컨테이너 exitCode/State 확인(워크플로가 자동 출력).
