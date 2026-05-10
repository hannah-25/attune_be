package attune.journal.application.dto.request;

import attune.journal.domain.model.SleepQuality;
import io.swagger.v3.oas.annotations.media.Schema;

public record CreateDailyStatusRequest(
        @Schema(description = "수면 시간 (시간 단위)", example = "7.5")
        @Min(0)
        @Max(24)
        Float sleepHour,

        @Schema(description = "수면 질 (GOOD/NORMAL/BAD)", example = "GOOD")
        SleepQuality sleepQuality,

        @Schema(description = "아침식사 여부", example = "true")
        Boolean ateBreakfast,

        @Schema(description = "점심식사 여부", example = "true")
        Boolean ateLunch,

        @Schema(description = "저녁식사 여부", example = "false")
        Boolean ateDinner
) {}
