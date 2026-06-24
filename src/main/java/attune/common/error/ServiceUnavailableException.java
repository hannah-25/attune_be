package attune.common.error;

public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message) { super(message); }
    public ServiceUnavailableException(String message, Throwable cause) { super(message, cause); }

    /** 클라이언트에게 반환할 사용자 친화적 메시지. 하위 클래스에서 도메인에 맞게 오버라이드한다. */
    public String getClientMessage() {
        return "서비스 이용이 일시적으로 원활하지 않습니다. 잠시 후 다시 시도해 주세요.";
    }
}
