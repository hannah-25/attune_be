package attune.journal.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateMemoRequest(
        @Schema(description = "메모 내용", example = "오늘은 약을 한 번 깜빡했다.")
        String memo
) {}
