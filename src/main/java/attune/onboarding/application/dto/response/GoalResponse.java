package attune.onboarding.application.dto.response;

import attune.journal.domain.model.DailyGoalType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record GoalResponse(
        @Schema(description = "저장된 목표 목록") List<GoalItem> goals
) {
    public record GoalItem(
            @Schema(description = "목표 ID") Long goalId,
            @Schema(description = "목표 제목") String title,
            @Schema(description = "기능 영역") DailyGoalType type,
            @Schema(description = "활성화 여부") boolean isActive
    ) {}
}
