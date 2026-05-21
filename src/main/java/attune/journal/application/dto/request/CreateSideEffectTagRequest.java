package attune.journal.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateSideEffectTagRequest(
        @Schema(description = "부작용 내용", example = "입마름", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String sideEffect,

        @Schema(description = "일지 날짜", example = "2026-05-09", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull LocalDate journalDate
) {}
