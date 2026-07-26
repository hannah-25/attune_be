package attune.common.error.toomanyrequests;

import attune.common.error.TooManyRequestsException;

public class NotificationReceiptRateLimitedException extends TooManyRequestsException {
    public NotificationReceiptRateLimitedException() {
        super("요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.");
    }
}
