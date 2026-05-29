package attune.medication.application.dto.response;

import attune.medication.domain.model.UserMedication;

public record CreateMedicationResponse(
        Long userMedicationId,
        String name,
        boolean isActive
) {
    public static CreateMedicationResponse from(UserMedication um) {
        return new CreateMedicationResponse(
                um.getId(),
                um.getMedicationDosage().getMedication().getName(),
                um.getIsActive()
        );
    }
}
