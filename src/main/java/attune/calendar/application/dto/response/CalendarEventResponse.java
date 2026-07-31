package attune.calendar.application.dto.response;

import attune.calendar.domain.model.CalendarProvider;
import attune.calendar.domain.model.ExternalCalendarEvent;
import attune.schedule.domain.model.Schedule;

import java.time.LocalDateTime;

public record CalendarEventResponse(
        String id,
        Long scheduleId,
        String source,
        CalendarProvider provider,
        String title,
        String description,
        String location,
        boolean isAllDay,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Long categoryId,
        String color,
        boolean editable
) {
    public static CalendarEventResponse fromSchedule(Schedule schedule, String color) {
        return new CalendarEventResponse(
                "schedule-" + schedule.getId(),
                schedule.getId(),
                "ATTUNE",
                null,
                schedule.getTitle(),
                schedule.getDescription(),
                schedule.getPlace(),
                schedule.isAllDay(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getScheduleCategoryId(),
                color,
                true
        );
    }

    public static CalendarEventResponse fromExternal(ExternalCalendarEvent event) {
        return new CalendarEventResponse(
                "external-" + event.getProvider().name().toLowerCase() + "-" + event.getId(),
                null,
                "EXTERNAL",
                event.getProvider(),
                event.getTitle(),
                event.getDescription(),
                event.getLocation(),
                event.isAllDay(),
                event.getStartTime(),
                event.getEndTime(),
                null,
                null,
                false
        );
    }
}
