package attune.consultation.application.dto.response;

import attune.consultation.domain.model.Consultation;

import java.time.LocalDateTime;

public record ConsultationUpdateResponse(
        Long consultationId,
        LocalDateTime consultationDate
) {
    public static ConsultationUpdateResponse from(Consultation consultation) {
        return new ConsultationUpdateResponse(
                consultation.getId(),
                consultation.getConsultationDate()
        );
    }
}
