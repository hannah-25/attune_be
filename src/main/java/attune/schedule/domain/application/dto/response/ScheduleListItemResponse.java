package attune.schedule.domain.application.dto.response;

import attune.schedule.domain.model.Schedule;
import attune.schedule.domain.model.ScheduleSource;

import java.time.LocalDateTime;

public record ScheduleListItemResponse(
        Long scheduleId,
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String color,
        ScheduleSource source
) {
    public static ScheduleListItemResponse of(Schedule schedule, String color) {
        return new ScheduleListItemResponse(
                schedule.getId(),
                schedule.getTitle(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                color,
                schedule.getSource()
        );
    }
}
