package attune.consultation.application.dto.response;

import attune.consultation.domain.model.Consultation;

import java.time.LocalDateTime;

public record ConsultationListItemResponse(
        LocalDateTime consultationDate,
        String place,
        String prescriptionNote
) {
    public static ConsultationListItemResponse from(Consultation consultation) {
        return new ConsultationListItemResponse(
                consultation.getConsultationDate(),
                consultation.getPlace(),
                consultation.getPrescriptionNote()
        );
    }
}
