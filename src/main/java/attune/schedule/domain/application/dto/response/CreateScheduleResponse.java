package attune.schedule.domain.application.dto.response;

import attune.schedule.domain.model.Schedule;

public record CreateScheduleResponse(
        Long scheduleId,
        String title
) {
    public static CreateScheduleResponse from(Schedule schedule) {
        return new CreateScheduleResponse(schedule.getId(), schedule.getTitle());
    }
}
