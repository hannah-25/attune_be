package attune.onboarding.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AsrsRequest(
        @NotEmpty @Valid
        @Schema(description = "ASRS 응답 목록")
        List<AnswerItem> answers
) {
    public record AnswerItem(
            @Schema(description = "문항 번호") int questionId,
            @Min(0) @Max(4) @Schema(description = "점수 (0-4)") int score
    ) {}
}
