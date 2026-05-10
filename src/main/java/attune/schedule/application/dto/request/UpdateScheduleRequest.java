package attune.schedule.application.dto.request;

import java.time.LocalDateTime;
import java.util.List;

public record UpdateScheduleRequest(
        String title,
        String description,
        Long categoryId,
        String place,
        Boolean isAllDay,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Boolean alarmEnabled,
        List<LocalDateTime> alarmedAt
) {}
