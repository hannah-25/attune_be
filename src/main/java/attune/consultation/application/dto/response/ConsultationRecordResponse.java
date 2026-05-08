package attune.consultation.application.dto.response;

import attune.consultation.domain.model.Consultation;

import java.time.LocalDateTime;

public record ConsultationRecordResponse(
        LocalDateTime consultationDate,
        String place,
        String doctorName,
        boolean isFirstVisit,
        String summaryReport,
        String preConsultationNote,
        String doctorAdvice,
        String prescriptionNote,
        String nextTreatmentGoal
) {
    public static ConsultationRecordResponse from(Consultation consultation) {
        return new ConsultationRecordResponse(
                consultation.getConsultationDate(),
                consultation.getPlace(),
                consultation.getDoctorName(),
                consultation.isFirstVisit(),
                consultation.getSummaryReport(),
                consultation.getPreConsultationNote(),
                consultation.getDoctorAdvice(),
                consultation.getPrescriptionNote(),
                consultation.getNextTreatmentGoal()
        );
    }
}
