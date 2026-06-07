package attune.ai.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GenerateTextRequest(
        @NotBlank String prompt
) {}
