package attune.consultation.application.dto.response;

import attune.consultation.domain.model.Consultation;

import java.time.LocalDateTime;

public record ConsultationScheduleResponse(
        Long consultationId,
        LocalDateTime consultationDate,
        String place
) {
    public static ConsultationScheduleResponse from(Consultation consultation) {
        return new ConsultationScheduleResponse(
                consultation.getId(),
                consultation.getConsultationDate(),
                consultation.getPlace()
        );
    }
}
