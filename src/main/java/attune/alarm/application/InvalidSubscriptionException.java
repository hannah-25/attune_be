package attune.alarm.application;

/**
 * 토큰 만료·무효 등 구독 자체가 영구적으로 사용 불가능한 경우 PushSender가 던진다.
 * 일시적 네트워크 오류 등 재시도 가능한 실패에는 사용하지 않는다.
 */
public class InvalidSubscriptionException extends RuntimeException {
    public InvalidSubscriptionException(String message) {
        super(message);
    }

    public InvalidSubscriptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
