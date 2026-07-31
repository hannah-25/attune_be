# 코딩 컨벤션

## 일반

- Java 17, Spring Boot 4. Lombok 사용(생성자/게터 등).
- 패키지/레이아웃은 [module-rules](../architecture/module-rules.md) 를 따른다.
- 생성자 주입 우선(필드 주입 지양). 단일 생성자면 `@Autowired` 생략 가능하나, 다중/특수 상황에선 명시한다.
  - 참고: 과거 `GeminiTextGenerator` 에서 주입 생성자에 `@Autowired` 누락으로 기동 실패가 있었다 → 모호하면 명시.
- 현재 사용자 조회는 항상 `SecurityUtils.getCurrentUserUuid()` (Service 레이어).

## 명명

- 컨트롤러 `*Controller`, 서비스 `*Service`, Repository `*Repository`, 엔티티는 도메인 명사.
- DTO는 `*Request`/`*Response`. 이벤트는 `*Event`, 리스너는 `*Listener`.
- 예외는 의미 + 기반 예외 접미사 패턴(예: `XxxNotFoundException`).

## 스타일 / 포맷

- 자동 포매터는 현재 도입하지 않는다(의도적). 주변 코드의 스타일(들여쓰기·네이밍·import 정렬)을 그대로 따른다.
- 기존 코드를 일괄 재포맷하지 않는다. **변경한 파일 범위**에서만 정리한다(거대 diff 금지).
- import 와일드카드 지양, 미사용 import 제거.

## 주석 / 문서화

- 공개 API는 `@Operation`/`@Schema` 로 의도를 보강.
- 비자명한 비즈니스 규칙·예외 케이스에 한해 주석. 코드로 표현 가능한 것은 코드로.

## 로깅

- 로그 패턴은 `application.yml` 정의를 따른다. 민감정보 마스킹.
- 디버그가 필요한 모듈은 프로파일/패키지 레벨로 조정(예: `logging.level.attune.calendar: DEBUG`).
