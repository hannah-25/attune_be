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
| 5xx 급증 | `GlobalExceptionHandler` 로그 | 최근 배포 revert 후보 |

## 롤백

- 코드: 문제 커밋 revert → develop 재배포.
- 컨테이너: 직전 이미지 태그로 `docker run`.

## 개선 후보 (tech-debt)

- health에 DB indicator 재활성 검토, 외부 의존(redis/smtp) 헬스 노출.
- 알림/지표 모니터링(metrics export) 강화.
