package attune.schedule.application.dto.response;

import attune.schedule.domain.model.Schedule;

public record UpdateScheduleResponse(
        Long scheduleId,
        String title
) {
    public static UpdateScheduleResponse from(Schedule schedule) {
        return new UpdateScheduleResponse(schedule.getId(), schedule.getTitle());
    }
}
