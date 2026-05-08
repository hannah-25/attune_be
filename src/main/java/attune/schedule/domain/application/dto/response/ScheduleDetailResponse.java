package attune.schedule.domain.application.dto.response;

import attune.schedule.domain.model.Schedule;

import java.time.LocalDateTime;

public record ScheduleDetailResponse(
        String title,
        String description,
        Long categoryId,
        String place,
        boolean isAllDay,
        LocalDateTime startTime,
        LocalDateTime endTime
) {
    public static ScheduleDetailResponse from(Schedule schedule) {
        return new ScheduleDetailResponse(
                schedule.getTitle(),
                schedule.getDescription(),
                schedule.getScheduleCategoryId(),
                schedule.getPlace(),
                schedule.isAllDay(),
                schedule.getStartTime(),
                schedule.getEndTime()
        );
    }
}
