package attune.common.error.notfound;

import attune.common.error.NotFoundException;

public class NotificationHistoryNotFoundException extends NotFoundException {
    public NotificationHistoryNotFoundException() {
        super("알림을 찾을 수 없습니다.");
    }
}
