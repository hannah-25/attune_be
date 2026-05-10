package attune.schedule.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record CreateScheduleRequest(
        @NotBlank String title,
        String description,
        @NotNull Long categoryId,
        String place,
        boolean isAllDay,
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime,
        boolean alarmEnabled,
        List<LocalDateTime> alarmedAt
) {}
