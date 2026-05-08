package attune.consultation.application.dto.response;

import attune.consultation.domain.model.Consultation;

public record ConsultationRecordResponse(
        String summaryReport,
        String preConsultationNote,
        String doctorAdvice,
        String prescriptionNote,
        String nextTreatmentGoal
) {
    public static ConsultationRecordResponse from(Consultation consultation) {
        return new ConsultationRecordResponse(
                consultation.getSummaryReport(),
                consultation.getPreConsultationNote(),
                consultation.getDoctorAdvice(),
                consultation.getPrescriptionNote(),
                consultation.getNextTreatmentGoal()
        );
    }
}
