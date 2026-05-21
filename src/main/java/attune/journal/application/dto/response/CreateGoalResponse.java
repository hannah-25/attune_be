package attune.journal.application.dto.response;

import attune.journal.domain.model.DailyGoal;

public record CreateGoalResponse(
        Long goalId,
        String content
) {
    public static CreateGoalResponse from(DailyGoal goal) {
        return new CreateGoalResponse(goal.getId(), goal.getDailyGoal());
    }
}
