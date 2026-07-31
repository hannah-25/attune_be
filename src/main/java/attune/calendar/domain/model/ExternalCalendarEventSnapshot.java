package attune.calendar.domain.model;

import java.time.LocalDateTime;

public record ExternalCalendarEventSnapshot(
        String providerCalendarId,
        String providerEventId,
        String title,
        String description,
        String location,
        boolean isAllDay,
        LocalDateTime startTime,
        LocalDateTime endTime,
        LocalDateTime providerUpdatedAt,
        boolean deleted,
        String rawEtag
) {
}
