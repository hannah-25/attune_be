# 실행 계획: dev 100 VU 부하 테스트

- 상태: completed
- 작성/실행: Codex / 2026-07-28

## 목표

`api-dev.attune-me.com`에서 100 동시 사용자 기준의 응답 시간·오류율·서버 자원 사용량을 측정하고, 전용 테스트 데이터와 관측 설정을 종료 후 정리한다.

## 실행 범위

- 80% 조회, 20% 복약 빠른 기록
- 100개의 `loadtest-*.attune.invalid` 전용 계정
- k6 단계: 1, 10, 25, 50, 75, 100 VU
- loadtest 프로파일에서만 EC2 loopback management port의 metrics 허용

## 최종 결과

| 동시 사용자 | HTTP 요청 | 오류율 | p95 |
| --- | ---: | ---: | ---: |
| 10 VU | 8,971 | 0% | 59.92 ms |
| 25 VU | 22,228 | 0% | 63.28 ms |
| 75 VU | 64,372 | 0% | 88.32 ms |
| 100 VU | 86,016 | 0% | 87.01 ms |

- 100 VU: 약 115.7 HTTP 요청/초, p99 2초 미만, 모든 k6 threshold 통과
- 100 VU 관측: Hikari pending 0, JVM heap 약 307 MB, 컨테이너 메모리 약 338 MiB, CPU 약 94%
- 결론: 측정한 오류율/지연시간 목표는 통과했다. DB 커넥션 풀 대기는 없었고, 현 인스턴스에서는 CPU가 다음 확장 한계 후보이다.

## 정리 및 복구

- `loadtest_data_action=cleanup` 배포로 전용 계정과 연관 데이터를 삭제했다.
- `loadtest=false`, `loadtest_data_action=none` 일반 dev 프로파일 복구 배포가 성공했다. (GitHub Actions run 30350024019)
- 종료된 로컬 k6 컨테이너를 삭제했다.

## 변경된 안전장치

- `loadtest` 프로파일에서만 `/actuator/metrics/**`를 허용하며, management server는 `127.0.0.1:8081`에 계속 바인딩된다.
- 배포 워크플로에서 seed/cleanup은 명시적 입력일 때만 실행된다.

## 완료 조건

- [x] 100 VU 결과와 서버 지표를 기록했다.
- [x] 전용 테스트 데이터와 컨테이너를 삭제했다.
- [x] dev를 일반 `dev` 프로파일로 복구했다.
