package attune.medication.application.dto.request;

import jakarta.validation.Valid;
import org.openapitools.jackson.nullable.JsonNullable;

import java.time.LocalDate;
import java.util.List;

public record UpdateMedicationRequest(
        JsonNullable<LocalDate> endAt,
        Boolean isActive,
        Boolean alarmActive,
        @Valid List<CreateMedicationRequest.ScheduleEntry> schedules
) {
    public UpdateMedicationRequest {
        if (endAt == null) {
            endAt = JsonNullable.undefined();
        }
    }
}
