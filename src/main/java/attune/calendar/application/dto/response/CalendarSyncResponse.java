package attune.calendar.application.dto.response;

import java.time.LocalDateTime;

public record CalendarSyncResponse(
        Long connectionId,
        LocalDateTime lastSyncedAt,
        int syncedCount
) {
}
