package attune.schedule.application.dto.response;

import attune.schedule.domain.model.Schedule;

import java.time.LocalDateTime;
import java.util.List;

public record ScheduleDetailResponse(
        String title,
        String description,
        Long categoryId,
        String place,
        boolean isAllDay,
        LocalDateTime startTime,
        LocalDateTime endTime,
        boolean alarmEnabled,
        List<LocalDateTime> alarms
) {
    public static ScheduleDetailResponse of(Schedule schedule, List<LocalDateTime> alarms) {
        return new ScheduleDetailResponse(
                schedule.getTitle(),
                schedule.getDescription(),
                schedule.getScheduleCategoryId(),
                schedule.getPlace(),
                schedule.isAllDay(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.isAlarmEnabled(),
                alarms
        );
    }
}
