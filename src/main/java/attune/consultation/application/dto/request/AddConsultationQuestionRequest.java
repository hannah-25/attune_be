package attune.consultation.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AddConsultationQuestionRequest(
        @NotBlank @jakarta.validation.constraints.Size(max = 255) String text
) {}
