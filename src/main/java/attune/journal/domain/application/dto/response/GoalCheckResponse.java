package attune.journal.domain.application.dto.response;

import attune.journal.domain.model.DailyGoal;
import attune.journal.domain.model.DailyGoalLog;

public record GoalCheckResponse(
        Long goalId,
        String content,
        Integer score
) {
    public static GoalCheckResponse of(DailyGoal goal, DailyGoalLog log) {
        return new GoalCheckResponse(goal.getId(), goal.getDailyGoal(), log.getScore());
    }
}
