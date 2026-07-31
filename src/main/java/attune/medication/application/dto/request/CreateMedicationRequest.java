package attune.medication.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record CreateMedicationRequest(
        Long consultationId,
        @NotNull Long medicationDosageId,
        @NotNull LocalDate startedAt,
        LocalDate endAt,
        @Valid @NotEmpty List<ScheduleEntry> schedules
) {
    public record ScheduleEntry(
            @NotNull LocalTime doseTime,
            String label
    ) {}
}
