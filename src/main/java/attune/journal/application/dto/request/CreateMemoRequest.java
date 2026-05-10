package attune.journal.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record CreateMemoRequest(
        @Schema(description = "메모 내용", example = "오늘은 컨디션이 좋았다.")
        String memo
) {}
