package attune.medication.domain.application.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.List;

public record UpdateMedicationScheduleRequest(
        @NotNull Boolean alarmActive,
        @NotEmpty List<ScheduleEntry> schedules
) {
    public record ScheduleEntry(
            @NotNull LocalTime doseTime,
            String label,
            @NotNull String dosage
    ) {}
}
