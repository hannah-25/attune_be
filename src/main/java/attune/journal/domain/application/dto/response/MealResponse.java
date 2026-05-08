package attune.journal.domain.application.dto.response;

import attune.journal.domain.model.DailyStatusLog;

public record MealResponse(
        Boolean ateBreakfast,
        Boolean ateLunch,
        Boolean ateDinner
) {
    public static MealResponse from(DailyStatusLog log) {
        if (log == null) return null;
        return new MealResponse(log.getAteBreakfast(), log.getAteLunch(), log.getAteDinner());
    }
}
