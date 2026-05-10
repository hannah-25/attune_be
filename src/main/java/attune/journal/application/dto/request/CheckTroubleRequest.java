package attune.journal.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CheckTroubleRequest(
        @Schema(description = "업무적 실수/불편 태그 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull Long tagId
) {}
