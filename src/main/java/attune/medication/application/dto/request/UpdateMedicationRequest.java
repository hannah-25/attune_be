package attune.medication.application.dto.request;

import org.openapitools.jackson.nullable.JsonNullable;

import java.time.LocalDate;

public record UpdateMedicationRequest(
        JsonNullable<LocalDate> endAt,
        Boolean isActive,
        Boolean alarmActive
) {
    public UpdateMedicationRequest {
        if (endAt == null) {
            endAt = JsonNullable.undefined();
        }
    }
}
