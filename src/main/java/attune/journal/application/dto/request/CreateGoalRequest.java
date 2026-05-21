package attune.journal.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateGoalRequest(
        @Schema(description = "일일 목표 내용 (50자 이내)", example = "할 일 미리 정리하기", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 50, message = "목표는 50자 이내여야 합니다.")
        String content,

        @Schema(description = "일지 날짜", example = "2026-05-09", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        LocalDate journalDate
) {}
