package attune.common.error.serviceunavailable;

import attune.common.error.ServiceUnavailableException;

/**
 * Google Calendar API가 일시적 오류(429 rate limit, 5xx)를 반환한 경우.
 * 클라이언트가 잠시 후 재시도할 수 있도록 503으로 매핑한다.
 */
public class GoogleCalendarUnavailableException extends ServiceUnavailableException {

    public GoogleCalendarUnavailableException(String message) {
        super(message);
    }

    @Override
    public String getClientMessage() {
        return "Google Calendar 연동이 일시적으로 원활하지 않습니다. 잠시 후 다시 시도해 주세요.";
    }
}
