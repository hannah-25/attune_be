package attune.journal.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ScoreGoalRequest(
        @Schema(description = "일일 목표 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull Long goalId,

        @Schema(description = "달성 점수 (0~10)", example = "8", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Min(value = 0, message = "점수는 0 이상이어야 합니다.")
        @Max(value = 10, message = "점수는 10 이하이어야 합니다.")
        Integer score
) {}
