# 실행 계획: dev 100 VU 부하 테스트

- 상태: active
- 작성자 / 날짜: Codex / 2026-07-28

## 목표

`dev.attune-me.com`에서 100 동시 사용자 기준선을 측정하고, 테스트 전용 데이터는 종료 뒤 삭제한다.

## 현재 상태

- dev management server는 `127.0.0.1:8081`에 바인딩된다.
- `loadtest` 프로파일은 로그를 낮추고 push를 stub으로 전환한다.
- metrics는 기본적으로 차단되어 있다.

## 변경 범위

- `loadtest` 활성화 때만 `/actuator/metrics/**`를 허용한다. loopback binding은 유지한다.
- `deploy-dev` 수동 실행에 `loadtest` 입력을 추가한다. push 배포는 기존 `dev` 단독 프로파일을 유지한다.
- 보안 회귀 테스트와 관측/배포 문서를 갱신한다.

## 제외 범위

- 운영 배포, public metrics 노출, 새 metrics exporter, 실제 push·메일·Gemini·Google Calendar 호출.

## 작업 단계

1. 변경을 검증·머지한다.
2. `deploy-dev`를 수동 실행하고 `loadtest=true`으로 dev를 재배포한다.
3. 전용 테스트 계정 100개와 각 계정의 복약·일지·일정 데이터를 만든다.
4. k6에서 1→10→25→50→75→100 VU를 각 10분간 실행한다. 조회 80%, 빠른 복약 기록 20%, 요청 간 1~3초 대기를 사용한다.
5. k6 결과(RPS, p50/p95/p99, 실패율·5xx), 15초 간격 readiness, 5초 간격 docker CPU/메모리, 서버 내부 Actuator JVM/Hikari metrics와 오류 로그를 수집한다.
6. 테스트 데이터만 삭제하고, `deploy-dev`를 수동 실행해 기본 `loadtest=false`로 dev 단독 프로파일을 복구한다.

## 검증 방법

- `./gradlew test --tests "attune.common.config.ActuatorSecurityTest" --tests "attune.common.config.LoadtestActuatorSecurityTest"`
- `scripts/agent/verify`
- loadtest 중 EC2 내부에서 `/actuator/metrics`, `/actuator/metrics/hikaricp.connections.active`, `/actuator/metrics/jvm.memory.used`가 200인지 확인한다.
- 외부에서는 management port가 열리지 않는지 확인한다.

## 위험 요소

- 테스트 계정이나 토큰, 사용자 콘텐츠를 결과물·로그에 남기지 않는다.
- 5xx가 1% 이상이거나 readiness가 실패하면 현재 단계를 중지하고 로그와 DB/Hikari 지표를 확인한다.

## 롤백 방법

- 수동 `deploy-dev`에서 `loadtest=false`로 재배포한다. metrics 허용은 비활성 프로파일에서 즉시 다시 차단된다.

## 완료 조건

- [ ] 100 VU 결과와 서버 지표가 기록됐다.
- [ ] 전용 테스트 데이터가 삭제됐다.
- [ ] dev가 `dev` 단독 프로파일로 복구됐다.
