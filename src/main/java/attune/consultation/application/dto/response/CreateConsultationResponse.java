package attune.consultation.application.dto.response;

import attune.consultation.domain.model.Consultation;

import java.time.LocalDateTime;

public record CreateConsultationResponse(
        Long consultationId,
        LocalDateTime consultationDate,
        String place,
        String doctorName,
        boolean isFirstVisit
) {
    public static CreateConsultationResponse from(Consultation consultation) {
        if (consultation == null) {
            return null;
        }

        return new CreateConsultationResponse(
                consultation.getId(),
                consultation.getConsultationDate(),
                consultation.getPlace(),
                consultation.getDoctorName(),
                consultation.isFirstVisit()
        );
    }
}
