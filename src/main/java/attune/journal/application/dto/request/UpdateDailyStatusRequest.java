package attune.journal.application.dto.request;

import attune.journal.domain.model.SleepQuality;
import io.swagger.v3.oas.annotations.media.Schema;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateDailyStatusRequest(
        @Schema(description = "수면 시간 (시간 단위, null로 전송 시 값 삭제)", example = "7.5")
        JsonNullable<Float> sleepHour,

        @Schema(description = "수면 질 (GOOD/NORMAL/BAD, null로 전송 시 값 삭제)", example = "NORMAL")
        JsonNullable<SleepQuality> sleepQuality,

        @Schema(description = "아침식사 여부 (null로 전송 시 값 삭제)", example = "true")
        JsonNullable<Boolean> ateBreakfast,

        @Schema(description = "점심식사 여부 (null로 전송 시 값 삭제)", example = "true")
        JsonNullable<Boolean> ateLunch,

        @Schema(description = "저녁식사 여부 (null로 전송 시 값 삭제)", example = "true")
        JsonNullable<Boolean> ateDinner
) {}
