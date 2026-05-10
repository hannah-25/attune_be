package attune.schedule.application.dto.response;

import attune.schedule.domain.model.Schedule;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public record UpdateAlarmsResponse(
        Long scheduleId,
        List<LocalDateTime> alarms
) {
    public static UpdateAlarmsResponse from(Schedule schedule) {
        List<LocalDateTime> alarms = schedule.getAlarmedAt() != null
                ? schedule.getAlarmedAt()
                : Collections.emptyList();
        return new UpdateAlarmsResponse(schedule.getId(), alarms);
    }
}
