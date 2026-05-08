package attune.journal.domain.application.dto.response;

import attune.journal.domain.model.DailyGoal;

public record GoalActiveResponse(
        Long goalId,
        String content
) {
    public static GoalActiveResponse from(DailyGoal goal) {
        return new GoalActiveResponse(goal.getId(), goal.getDailyGoal());
    }
}
