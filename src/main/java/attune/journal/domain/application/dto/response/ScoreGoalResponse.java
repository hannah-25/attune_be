package attune.journal.domain.application.dto.response;

import attune.journal.domain.model.DailyGoalLog;

import java.time.LocalDate;

public record ScoreGoalResponse(
        Long goalId,
        Integer score,
        LocalDate journalDate
) {
    public static ScoreGoalResponse from(DailyGoalLog log) {
        return new ScoreGoalResponse(log.getDailyGoalId(), log.getScore(), log.getDate());
    }
}
