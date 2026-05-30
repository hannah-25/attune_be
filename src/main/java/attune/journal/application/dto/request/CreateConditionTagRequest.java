package attune.journal.application.dto.request;

import attune.journal.domain.model.ConditionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateConditionTagRequest(
        @Schema(description = "감정/증상 내용", example = "머리 맑음", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String condition,

        @Schema(description = "감정/증상 유형 (UP=기분 좋음, DOWN=다운, TIGHT=긴장, FOGGY=멍함, CALM=차분, USER_INPUT=직접 입력)",
                example = "UP", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull ConditionType conditionType,

        @Schema(description = "일지 날짜", example = "2026-05-09", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull LocalDate journalDate
) {}
