package attune.medication.application.dto.response;

import attune.medication.domain.model.UserMedication;

import java.time.LocalDateTime;

public record UpdateMedicationResponse(
        Long medicationId,
        boolean isActive,
        LocalDateTime updatedAt
) {
    public static UpdateMedicationResponse from(UserMedication um) {
        return new UpdateMedicationResponse(
                um.getId(),
                um.getIsActive(),
                um.getUpdatedAt()
        );
    }
}
