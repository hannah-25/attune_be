# 아키텍처

Spring Boot 4 / Java 17 기반의 **헥사고날(포트 & 어댑터) 아키텍처** 애플리케이션이다. 모든 도메인 모듈은 동일한 내부 구조를 따른다:

```
attune/<domain>/
  domain/
    adapter/web/      ← REST 컨트롤러 (HTTP 인바운드 어댑터)
    application/      ← 서비스 클래스 + DTO (유스케이스 레이어)
    model/            ← JPA 엔티티 및 열거형
    repository/       ← Spring Data JPA 인터페이스 (아웃바운드 포트)
```

## 모듈

| 모듈 | 역할 |
|------|------|
| `auth` | JWT 발급, 갱신, 로그아웃; Redis 기반 토큰 캐시 |
| `user` | 계정 관리, 설정, 이메일 인증, 비밀번호 재설정 |
| `term` | 약관 버전 관리 및 사용자 동의 이력 추적 |
| `communityBoard` | 커뮤니티 게시글 (`feat/board` 브랜치에서 개발 중) |
| `medication` | 약물 마스터, 사용자 복약 정보, 스케줄, 복약 로그 |
| `notice` | 공지사항 |
| `common` | 공통 관심사: `SecurityConfig`, `JwtAuthenticationFilter`, `JwtProvider`, `GlobalExceptionHandler`, 공유 유틸 |
