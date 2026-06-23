package attune.consultation.application.dto.response;

import attune.consultation.domain.model.Consultation;

import java.time.LocalDateTime;
import java.util.Objects;

public record CreateConsultationResponse(
        Long consultationId,
        LocalDateTime consultationDate,
        String place,
        String doctorName,
        boolean isFirstVisit
) {
    public static CreateConsultationResponse from(Consultation consultation) {
        Objects.requireNonNull(consultation, "consultation must not be null");

        return new CreateConsultationResponse(
                consultation.getId(),
                consultation.getConsultationDate(),
                consultation.getPlace(),
                consultation.getDoctorName(),
                consultation.isFirstVisit()
        );
    }
}
