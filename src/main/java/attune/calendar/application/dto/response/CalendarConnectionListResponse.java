package attune.calendar.application.dto.response;

import java.util.List;

public record CalendarConnectionListResponse(
        List<CalendarConnectionResponse> connections
) {
}
