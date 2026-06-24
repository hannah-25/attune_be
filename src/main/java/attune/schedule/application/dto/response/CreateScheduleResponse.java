package attune.schedule.application.dto.response;

import attune.schedule.domain.model.Schedule;

import java.time.LocalDateTime;

public record CreateScheduleResponse(
        Long scheduleId,
        String title,
        Long categoryId,
        boolean isAllDay,
        LocalDateTime startTime,
        LocalDateTime endTime
) {
    public static CreateScheduleResponse from(Schedule schedule) {
        if (schedule == null) {
            return null;
        }

        return new CreateScheduleResponse(
                schedule.getId(),
                schedule.getTitle(),
                schedule.getScheduleCategoryId(),
                schedule.isAllDay(),
                schedule.getStartTime(),
                schedule.getEndTime()
        );
    }
}
