# 예외 처리 규칙

## 예외 계층

`common/error` 에 HTTP 상태별 **기반 예외**가 있다:

| 기반 예외 | HTTP |
|-----------|------|
| `BadRequestException` | 400 |
| `UnauthorizedException` | 401 |
| `NotFoundException` | 404 |
| `ConflictException` | 409 |
| `ServiceUnavailableException` | 503 |
| `InternalServerException` | 500 |

도메인별 구체 예외는 `common/error/{badrequest,unauthorized,notfound,conflict,...}` 하위에 두고 해당 기반 클래스를 상속한다.

## 규칙

1. 새 예외는 위 기반 클래스를 상속하고 적절한 하위 패키지에 둔다. 임의 RuntimeException으로 흘려보내지 않는다.
2. 컨트롤러/서비스에서 `try-catch` 로 상태코드를 직접 만들지 말고, 예외를 던지고 `GlobalExceptionHandler` 가 매핑하게 한다.
3. 응답 본문은 `ErrorResponse` 로 통일한다.
4. 예외 메시지에 **secret·토큰·개인정보·스택 내부 경로**를 노출하지 않는다.
5. 외부 연동 실패(메일·푸시·Gemini)는 적절히 `ServiceUnavailableException`/도메인 예외로 변환하고 로깅한다.

## 강제 수단

- ArchUnit `attune.architecture.ErrorHandlingTest`(soft): 커스텀 예외가 `common/error` 의 기반 예외를 상속하는지 점검.
- 신규 예외 매핑 누락은 PR 리뷰에서 확인.
