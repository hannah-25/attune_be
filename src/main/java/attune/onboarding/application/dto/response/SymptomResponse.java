package attune.onboarding.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record SymptomResponse(
        @Schema(description = "증상 ID") Long symptomId,
        @Schema(description = "저장 시각") LocalDateTime savedAt
) {}
