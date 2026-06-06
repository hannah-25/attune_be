package attune.schedule.application.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record UpdateAlarmsResponse(
        Long scheduleId,
        List<LocalDateTime> alarms
) {}
