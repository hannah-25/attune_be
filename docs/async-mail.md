# 비동기 처리 & 메일

- 이메일 발송은 `@Async`(`@EnableAsync`로 활성화)를 사용한다.
- `JavaMailSender`는 Gmail SMTP로 설정되어 있다.
- 회원가입 후 웰컴 메일은 `ApplicationEventPublisher`를 통해 발행된다.
