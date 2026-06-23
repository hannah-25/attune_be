package attune.onboarding.application.dto.request;

import attune.journal.domain.model.DailyGoalType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record GoalRequest(
        @NotEmpty @Valid
        @Schema(description = "확정 목표 목록 (4개)")
        List<GoalItem> goals,

        @Schema(description = "visible로 설정할 journal tag ID 목록")
        List<Long> visibleTagIds
) {
    public record GoalItem(
            @NotBlank @Schema(description = "목표 제목") String title,
            @NotNull @Schema(description = "기능 영역") DailyGoalType type
    ) {}
}
