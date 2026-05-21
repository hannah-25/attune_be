package attune.journal.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateGoalRequest(
        @Schema(description = "수정할 목표 내용 (50자 이내)", example = "오늘 할 일 목록 작성하기", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 50, message = "목표는 50자 이내여야 합니다.")
        String content
) {}
