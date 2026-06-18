package attune.consultation.application.dto.response;

import attune.consultation.domain.model.ConsultationQuestion;

public record ConsultationQuestionResponse(
        Long questionId,
        String text
) {
    public static ConsultationQuestionResponse from(ConsultationQuestion question) {
        return new ConsultationQuestionResponse(question.getId(), question.getText());
    }
}
