package attune.consultation.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AddConsultationQuestionRequest(
        @NotBlank String text
) {}
