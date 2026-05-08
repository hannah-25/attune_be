package attune.journal.domain.application.dto.response;

import attune.journal.domain.model.DailyStatusLog;
import attune.journal.domain.model.SleepQuality;

public record SleepResponse(
        Float sleepHour,
        SleepQuality sleepQuality
) {
    public static SleepResponse from(DailyStatusLog log) {
        if (log == null) return null;
        return new SleepResponse(log.getSleepHour(), log.getSleepQuality());
    }
}
