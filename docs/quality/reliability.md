# 신뢰성 / 장애 대응

## 헬스 / 가용성

- Liveness/Readiness 게이트: `/actuator/health` (배포 시 600s 폴링).
- 주의: health의 `db`·`diskspace` 인디케이터가 꺼져 있어 **DB 다운이 health 200을 막지 않는다.** DB 장애는 기능 오류/로그로 드러난다 → 별도 모니터링 필요(개선 후보).

## 장애 시나리오 → 대응

| 증상 | 1차 확인 | 대응 |
|------|----------|------|
| 앱 부팅 실패 | 부팅 로그(빈 생성/주입) | 생성자 `@Autowired`/설정/시크릿 누락 확인 (과거 GeminiTextGenerator 사례) |
| 컨테이너 즉시 종료 | `docker logs --tail 300` | exitCode/State, 프로파일/환경변수 점검 |
| DB 연결 실패 | Hikari/Hibernate 로그 | 자격증명/네트워크/스키마. health엔 안 잡힘 주의 |
| Redis 실패 | 로그인/토큰 갱신 오류 | Redis 가용성, 토큰 캐시 우회 영향 |
| 메일/푸시 미발송 | 비동기 발송 로그, 발송 이력 | provider 설정(stub vs web-push), SMTP/VAPID |
| Gemini 오류 | 응답 검증기 로그 | 키/쿼터/타임아웃. 실패 시 도메인 예외 변환 확인 |
| Calendar sync 실패 | `attune.calendar.requests` outcome, `GoogleCalendarClient` warn/error 로그 | outcome=reauth면 사용자 재연동 필요(400 정상 동작), unavailable이면 Google 측 일시 장애(503) — 서버 조치 불필요 |
| 5xx 급증 | `GlobalExceptionHandler` 로그 | 최근 배포 revert 후보 |

## 외부 API 장애 정책

외부 의존성별 timeout·재시도·오류 분류 기준. 공통 원칙: **사용자 조치로 해결되는 실패(재연동·재시도)는 4xx/503으로 구분해 안내하고, 응답 body는 PII 보호를 위해 로그·예외에 보존하지 않는다.**

| 대상 | timeout | 재시도 | rate limit(429) | 오류 분류 | 메트릭 |
|------|---------|--------|-----------------|-----------|--------|
| Gemini (`GeminiTextGenerator`) | 5s/30s | 502·503·504·연결 실패만 1회+300ms 백오프 | 재시도 없이 즉시 503 (재시도해도 회복 안 되고 비용만 발생) | 전이성 오류 소진 → 503(`GeminiUnavailableException`), 그 외 → 500 | `attune.gemini.requests`, `attune.gemini.duration` (outcome: success/quota/failure) |
| Google Calendar (`GoogleCalendarClient`) | oauthRestClient 5s/10s | **없음** — sync는 사용자 트리거 동작이라 실패를 즉시 알리고 재시도 판단을 사용자에게 넘긴다 | 503(`GoogleCalendarUnavailableException`) + `Retry-After` | 401/403·token 4xx → 400 재연동 안내(`CalendarReauthRequiredException`), 429/5xx/연결 실패 → 503, 그 외 4xx → 500 | `attune.calendar.requests` (operation: token/userinfo/calendar_list/events, outcome: success/reauth/unavailable/failure) |
| Web Push (`WebPushSender`) | connect/socket/connectionRequest 각 5s | 스케줄러 레벨 MAX_RETRY=3, 1시간 recovery window | 일반 실패로 취급(스케줄러 재시도로 흡수) | 404/410 → 구독 비활성화(`InvalidSubscriptionException`), 그 외 → 실패 후 재시도 | `attune.push.requests` (outcome: success/invalid_subscription/failure) |
| SMTP 메일 (`MailService`) | Spring Mail 기본값 | **없음** (비동기 발송, 실패 시 로그만) — 개선 후보는 tech-debt-tracker 참고 | - | `MessagingException` → `MailSendFailedException`(500). SMTP 오류 메시지에 수신자 주소가 포함될 수 있어 원본 예외는 sanitize 후 보존 | `attune.mail.requests` (type/outcome) |

## 롤백

- 코드: 문제 커밋 revert → develop 재배포.
- 컨테이너: 직전 이미지 태그로 `docker run`.

## 개선 후보 (tech-debt)

- health에 DB indicator 재활성 검토, 외부 의존(redis/smtp) 헬스 노출.
- 알림/지표 모니터링(metrics export) 강화.
