# 비동기 처리 & 메일

- 이메일 발송은 `@Async`(`@EnableAsync`로 활성화)를 사용한다.
- `JavaMailSender`는 Gmail SMTP로 설정되어 있다.
- 회원가입 후 웰컴 메일은 `ApplicationEventPublisher`를 통해 발행된다.

## 스레드 풀

`AsyncConfig`에서 `AsyncConfigurer`를 구현하여 `@Async` 전용 스레드 풀을 설정한다.

| 설정 | 값 |
|---|---|
| corePoolSize | 2 |
| maxPoolSize | 10 |
| queueCapacity | 500 |
| threadNamePrefix | async- |

## 약관 변경 메일 배치 발송

`MailEventListener.handleTermsUpdated()`는 활성 사용자를 한 번에 전체 조회하지 않고
500명 단위 페이지로 나누어 순차 발송한다(`Page<User>` + `pageable.next()` 루프).
이로써 대규모 사용자 환경에서 OOM 없이 발송할 수 있다.
