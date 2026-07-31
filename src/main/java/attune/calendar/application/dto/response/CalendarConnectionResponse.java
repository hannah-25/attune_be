package attune.calendar.application.dto.response;

import attune.calendar.domain.model.CalendarConnection;
import attune.calendar.domain.model.CalendarProvider;

import java.time.LocalDateTime;
import java.util.List;

public record CalendarConnectionResponse(
        Long connectionId,
        CalendarProvider provider,
        String accountEmail,
        List<String> selectedCalendarIds,
        LocalDateTime lastSyncedAt,
        boolean active
) {
    public static CalendarConnectionResponse from(CalendarConnection connection) {
        return new CalendarConnectionResponse(
                connection.getId(),
                connection.getProvider(),
                connection.getAccountEmail(),
                connection.selectedCalendarIdList(),
                connection.getLastSyncedAt(),
                connection.isActive()
        );
    }
}
