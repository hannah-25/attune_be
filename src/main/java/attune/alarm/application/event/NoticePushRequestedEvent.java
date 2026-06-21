package attune.alarm.application.event;

import java.time.LocalDateTime;

public record NoticePushRequestedEvent(
        Long noticeId,
        String title,
        String content,
        LocalDateTime scheduledAt
) {
}
