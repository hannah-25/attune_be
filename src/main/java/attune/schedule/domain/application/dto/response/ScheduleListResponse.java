package attune.schedule.domain.application.dto.response;

import java.util.List;

public record ScheduleListResponse(
        List<ScheduleListItemResponse> schedules
) {}
