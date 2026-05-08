package attune.consultation.application.dto.request;

public record UpdateConsultationResultRequest(
        String doctorAdvice,
        String prescriptionNote,
        String nextTreatmentGoal
) {}
