package attune.schedule.domain.application.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record UpdateAlarmsRequest(
        @NotNull Boolean alarmEnabled,
        List<LocalDateTime> alarmedAt
) {}
