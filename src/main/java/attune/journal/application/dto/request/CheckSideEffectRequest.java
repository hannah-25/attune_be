package attune.journal.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CheckSideEffectRequest(
        @Schema(description = "부작용 태그 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull Long tagId
) {}
