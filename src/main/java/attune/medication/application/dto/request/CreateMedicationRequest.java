package attune.medication.application.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreateMedicationRequest(
        @NotNull Long medicationId,
        Long hospitalId,
        @NotNull LocalDate startedAt,
        LocalDate endAt,
        @NotEmpty List<ScheduleEntry> schedules
) {
    public record ScheduleEntry(
            @NotNull java.time.LocalTime doseTime,
            String label,
            @NotNull String dosage
    ) {}
}
