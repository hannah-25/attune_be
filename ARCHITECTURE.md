# ARCHITECTURE.md — Attune 백엔드

> 시스템 구조의 **단일 진입점**. 상세 규칙은 [`docs/architecture/`](./docs/architecture/index.md) 로 링크한다.
> 이 문서는 "지금 어떤 구조인가(현재 상태)"를 설명하고, 강제 규칙은 [module-rules](./docs/architecture/module-rules.md) 등에서 다룬다.

## 1. 스타일

**헥사고날(포트 & 어댑터)** 아키텍처. 모든 도메인 모듈은 동일한 내부 레이아웃을 따른다:

```
attune/<domain>/
  adapter/
    web/            ← REST 컨트롤러 (HTTP 인바운드 어댑터)
    <기타>/          ← oauth, gemini, event 등 외부 연동 어댑터 (도메인에 따라)
  application/
    <Service>.java  ← 유스케이스 (트랜잭션 경계)
    dto/            ← 요청/응답 DTO
    port/           ← (선택) 아웃바운드 포트 인터페이스
    event/          ← (선택) 애플리케이션 이벤트
    error/          ← (선택) 도메인 특화 예외
  domain/
    model/          ← JPA 엔티티, 열거형, 값 객체
    repository/     ← Spring Data JPA 인터페이스 (아웃바운드 포트)
  config/           ← (선택) 모듈 전용 설정
  infrastructure/   ← (선택) 외부 시스템 구현 (예: Gemini client)
```

> 의존 방향: `adapter → application → domain`. domain은 위 계층을 모른다.
> 횡단 관심사(보안/예외/캐시/공통 설정)는 `attune.common` 에 모은다.

## 2. 의존성 흐름

```
[HTTP] → adapter/web (Controller)
            → application (Service, @Transactional)
                → domain/repository (Repository 포트)
                    → domain/model (Entity)
                → adapter|infrastructure (외부 API: Gemini, web-push, Mail)
```

- 컨트롤러는 비즈니스 로직을 갖지 않고 Service에 위임한다.
- Service는 트랜잭션·유스케이스 조립을 담당한다. 현재 사용자 UUID는 `SecurityUtils.getCurrentUserUuid()` (Service 레이어에서 호출).
- Repository는 데이터 접근만 담당하고 비즈니스 정책을 갖지 않는다.
- 외부 API 호출은 `adapter/<vendor>` 또는 `infrastructure/` 로 격리한다.
- **OSIV 비활성화**: 지연 로딩은 트랜잭션(서비스 메서드) 내에서만. 필요한 연관은 쿼리에서 미리 로딩한다.

상세·강제 규칙: [dependency-rules](./docs/architecture/dependency-rules.md), [module-rules](./docs/architecture/module-rules.md).

## 3. 도메인 모듈 (현재)

| 모듈 | 역할 |
|------|------|
| `auth` | JWT 발급·갱신·로그아웃, 소셜 OAuth, Redis 토큰 캐시(`UserAuthCache`) |
| `user` | 계정 관리, 설정, 이메일 인증, 비밀번호 재설정, 탈퇴(소프트/영구) |
| `onboarding` | 가입 직후 온보딩(조건 태그 추천 등) |
| `term` | 약관 버전 관리 및 사용자 동의 이력 |
| `journal` | 일지: Condition/SideEffect/Trouble 태그 + 일별 체크인 로그, DailyStatus, Memo, DailyGoal |
| `medication` | 약물 마스터, 사용자 복약 정보, 스케줄, 복약 로그 |
| `medicationAnalysis` | 약물 치료 경과 리포트: 복용 통계, 날짜 비교, 처방 변경 감지, Gemini 요약 |
| `consultation` | 진료 일정·기록 관리 |
| `schedule` | 일정 관리 |
| `calendar` | 캘린더 통합 뷰 |
| `todo` | 할 일 관리 |
| `notice` | 공지사항 |
| `support` | 고객 지원/문의 |
| `communityBoard` | 커뮤니티 게시글·댓글 |
| `alarm` | 푸시 알람 구독·발송 이력·스케줄러(복약/일정/Todo/리포트)·이벤트 리스너 |
| `ai` | 공통 AI(Gemini) 어댑터·유스케이스 |
| `admin` | 어드민: 회원 관리, 마케팅 발송, 감사 로그(audit) |
| `common` | SecurityConfig, JwtAuthenticationFilter, JwtProvider, GlobalExceptionHandler, 공유 설정·유틸 |

> 이 표는 현재 코드 기준이다. `scripts/agent/generate-project-map` 으로 [`docs/generated/project-map.md`](./docs/generated/project-map.md) 를 갱신할 수 있다.

## 4. 횡단 관심사

| 관심사 | 위치 |
|--------|------|
| 인증/인가 | `common/security`, `common/filter`(JwtAuthenticationFilter), `common/config/SecurityConfig` |
| 예외 처리 | `common/error` (`GlobalExceptionHandler` + HTTP 상태별 기반 예외) |
| 캐시 | `common/config/CacheConfig` (Caffeine), Redis(토큰) |
| 비동기/메일 | `common/config/AsyncConfig`, `common/mail` |
| 직렬화 | `common/config/JacksonConfig` |
| API 문서 | `common/config/OpenApiConfig` (springdoc) |
| CORS | `common/config/CorsProperties` + SecurityConfig |

## 5. 알려진 드리프트 / 주의

- `docs/architecture.md`(구버전)는 모듈 일부만 나열하고 레이아웃 표기가 낡았다 → 본 문서가 정본. 정리 대상은 [`docs/exec-plans/tech-debt-tracker.md`](./docs/exec-plans/tech-debt-tracker.md).
- 일부 도메인은 `port/`, `infrastructure/`, `application/error/` 를 갖고 일부는 갖지 않는다(선택적). 새 모듈은 가장 단순한 형태에서 필요할 때 추가한다.

상세 규칙 색인: [`docs/architecture/index.md`](./docs/architecture/index.md)
