package attune.onboarding.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record GoalRequest(
        @NotEmpty @Valid
        @Schema(description = "목표 목록")
        List<GoalItem> goals
) {
    public record GoalItem(
            @NotBlank @Schema(description = "목표 제목") String title,
            @Schema(description = "목표 설명 (선택)") String description
    ) {}
}