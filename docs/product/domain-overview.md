# 도메인 개요

각 도메인의 책임과 주요 데이터. 코드 위치는 `src/main/java/attune/<domain>/`.
모듈 책임 표는 [`ARCHITECTURE.md`](../../ARCHITECTURE.md) 와 동기화한다.

## 사용자/인증 영역

- **auth** — 로그인, JWT 발급/갱신/로그아웃, 소셜 OAuth. Redis에 토큰 캐시(`UserAuthCache`).
- **user** — 계정, 설정, 이메일 인증, 비밀번호 재설정, 탈퇴(소프트 → 영구). 타임존 지원.
- **onboarding** — 가입 직후 조건 태그 추천 등 초기 설정.
- **term** — 약관 버전 + 동의 이력.

## 기록/건강 영역

- **journal** — 일지의 핵심. `*Tag`(활성 태그) + `*Log`(날짜별 체크인) 2계층. Condition/SideEffect/Trouble, DailyStatus, Memo, DailyGoal.
- **medication** — 약물 마스터, 사용자 복약 정보, 복약 스케줄, 복약 로그.
- **medicationAnalysis** — 복용 통계, 기간 비교, 처방 변경 감지, Gemini 요약 리포트.
- **consultation** — 진료 일정·기록.
- **schedule / calendar / todo** — 일정·캘린더·할 일.

## 커뮤니케이션 영역

- **alarm** — 푸시 구독, 발송 이력, 스케줄러(복약·일정·Todo·리포트), 댓글 등 이벤트 리스너.
- **notice** — 공지사항.
- **support** — 문의/지원.
- **communityBoard** — 게시글·댓글.

## 플랫폼/공통

- **ai** — 공통 Gemini 어댑터·유스케이스.
- **admin** — 회원 관리, 마케팅 발송, 감사 로그(audit, HMAC 해시 보관).
- **common** — 보안/예외/캐시/메일/직렬화/문서 등 횡단 관심사.

## 데이터 모델 관례 (journal 예시)

`ConditionTag`(사용자 활성 태그) ↔ `ConditionLog`(날짜별 기록)를 JOIN해 일지를 조회한다.
Repository에 `findAllInRangeWithTag` 형태의 커스텀 쿼리가 있다. 전체 스키마는 [`db_schema.md`](../db_schema.md).
