package attune.calendar.application.dto.response;

import java.util.List;

public record CalendarEventListResponse(
        List<CalendarEventResponse> events
) {
}
