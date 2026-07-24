package attune.alarm.application.dto.response;

import attune.alarm.domain.model.NotificationAlarmType;
import attune.alarm.domain.model.NotificationHistory;
import attune.alarm.domain.model.NotificationStatus;

import java.time.LocalDateTime;

public record NotificationInboxItemResponse(
        Long id,
        NotificationAlarmType alarmType,
        String title,
        String body,
        String url,
        NotificationStatus status,
        NotificationDeliveryStatus deliveryStatus,
        LocalDateTime sentAt,
        LocalDateTime readAt
) {
    public static NotificationInboxItemResponse from(
            NotificationHistory history,
            NotificationDeliveryStatus deliveryStatus
    ) {
        return new NotificationInboxItemResponse(
                history.getId(),
                history.getAlarmType(),
                history.getTitle(),
                history.getBody(),
                history.getUrl() == null ? "/home" : history.getUrl(),
                history.getStatus(),
                deliveryStatus,
                history.getSentAt(),
                history.getReadAt()
        );
    }
}
