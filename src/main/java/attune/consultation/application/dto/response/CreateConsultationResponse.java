package attune.consultation.application.dto.response;

import attune.consultation.domain.model.Consultation;

import java.time.LocalDateTime;

public record CreateConsultationResponse(
        Long consultationId,
        LocalDateTime consultationDate,
        String place
) {
    public static CreateConsultationResponse from(Consultation consultation) {
        return new CreateConsultationResponse(
                consultation.getId(),
                consultation.getConsultationDate(),
                consultation.getPlace()
        );
    }
}
