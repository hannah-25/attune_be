package attune.medication.domain.application.dto.response;

import attune.medication.domain.model.UserMedication;

public record CreateMedicationResponse(
        Long medicationId,
        String name,
        boolean isActive
) {
    public static CreateMedicationResponse from(UserMedication um) {
        return new CreateMedicationResponse(
                um.getId(),
                um.getMedication().getName(),
                um.getIsActive()
        );
    }
}
