package attune.medication.application.dto.request;

import java.time.LocalDate;

public record UpdateMedicationRequest(
        LocalDate endAt,
        Boolean isActive,
        Boolean alarmActive
) {}
