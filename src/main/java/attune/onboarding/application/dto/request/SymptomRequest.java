package attune.onboarding.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record SymptomRequest(
        @NotBlank @Schema(description = "초기 증상 서술") String description,
        @Schema(description = "감정적 사건 서술") String emotionalEvent
) {}
