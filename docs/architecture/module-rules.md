# 모듈 내부 레이아웃 규칙

각 도메인 모듈은 동일한 레이아웃을 따른다. 일관성은 에이전트가 위치를 추측하지 않게 한다.

## 규칙

1. **컨트롤러는 `adapter/web` 에만** 둔다. `@RestController`/`@Controller` 가 다른 패키지에 있으면 안 된다.
2. **서비스(`*Service`)는 `application` 에** 둔다. 트랜잭션 경계는 여기.
3. **요청/응답 DTO는 `application/dto`** (필요 시 `dto/request`, `dto/response`).
4. **엔티티·열거형은 `domain/model`**, **Repository 인터페이스는 `domain/repository`**.
5. 외부 시스템 연동 구현은 **`adapter/<vendor>` 또는 `infrastructure/`** 로 격리한다 (예: `ai/adapter/gemini`).
6. 모듈 전용 설정은 `config/`, 아웃바운드 포트 인터페이스는 필요 시 `application/port/`.
7. 모듈 간 직접 의존은 최소화한다. 공유는 `common` 또는 명시적 포트를 통한다.

## 강제 수단

- ArchUnit 테스트 `attune.architecture.ModuleLayoutTest` 로 1·2·4 를 검증한다(soft: 위반 시 경고/추적).
- 신규 모듈은 가장 단순한 형태(`adapter/web`, `application`, `domain/model`, `domain/repository`)로 시작하고, 필요할 때만 `port/`·`infrastructure/`·`event/` 를 추가한다.

## 현재 상태 (참고)

대부분 모듈이 규칙을 따른다. 일부 모듈만 `port/`, `application/error/`, `infrastructure/` 를 갖는다(선택적, 위반 아님).
