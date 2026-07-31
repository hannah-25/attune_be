# 의존성 방향 규칙

## 핵심 규칙

의존은 **바깥 → 안쪽** 한 방향이다:

```
adapter (web/외부어댑터)  →  application (서비스/DTO)  →  domain (model/repository)
```

1. `domain..` 패키지는 `adapter..`, `application..` 를 **import 하지 않는다**. (도메인은 바깥을 모른다)
2. `domain.repository..` 는 `adapter..` 에 의존하지 않는다.
3. 컨트롤러(`adapter.web`)는 Repository를 직접 호출하지 않고 Service를 거친다. (soft rule — 단순 조회 예외는 리뷰에서 판단)
4. 외부 API/SDK 의존은 `adapter/<vendor>`·`infrastructure` 안에만 둔다. application/domain은 포트 인터페이스에만 의존한다.
5. **순환 의존 금지**: 모듈 간 사이클이 없어야 한다.

## 강제 수단

- ArchUnit `attune.architecture.DependencyRulesTest`:
  - `domain..` → `adapter..` 의존 금지 (강제)
  - `domain..` → `application..` 의존 금지 (강제)
  - 슬라이스 순환 의존 금지 (soft: 위반 발견 시 tech-debt-tracker 등록 후 강제 승격)

## 예외/주의

- `attune.common` 은 모든 계층이 의존할 수 있는 공유 커널이다. 단, `common` 이 특정 도메인 정책을 갖지 않게 한다.
- 위반이 불가피하면 [tech-debt-tracker](../exec-plans/tech-debt-tracker.md) 에 사유와 함께 기록한다.
