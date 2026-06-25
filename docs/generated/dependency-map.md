<!-- 부분 자동/수동 혼합. 의존 규칙의 권위는 ArchUnit 테스트다. -->
# 의존성 지도

권위 있는 검증: `src/test/java/attune/architecture/DependencyRulesTest.java` (ArchUnit).
규칙 설명: [`../architecture/dependency-rules.md`](../architecture/dependency-rules.md).

## 허용 방향

```
adapter (web/oauth/gemini/event)
   ↓
application (service, dto, port, event)
   ↓
domain (model, repository)

common  ← 모든 계층이 의존 가능 (공유 커널)
```

## 강제되는 규칙 (ArchUnit)

- `..domain..` 는 `..adapter..` 에 의존하지 않는다.
- `..domain..` 는 `..application..` 에 의존하지 않는다.
- 컨트롤러는 `..adapter.web..` 에만 존재한다.
- Repository 인터페이스는 `..domain.repository..` 에만 존재한다.

## soft (추적 중, tech-debt-tracker TD-4)

- 컨트롤러 → Repository 직접 호출 금지.
- 모듈(슬라이스) 간 순환 의존 금지.

> 모듈별 파일 수 개요는 [project-map.md](./project-map.md) 참고.
