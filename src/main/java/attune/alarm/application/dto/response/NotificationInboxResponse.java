package attune.alarm.application.dto.response;

import java.util.List;

public record NotificationInboxResponse(
        List<NotificationInboxItemResponse> notifications,
        String nextCursor
) {
}
