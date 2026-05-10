package attune.journal.application.dto.response;

import attune.journal.domain.model.DailyStatusLog;
import attune.journal.domain.model.SleepQuality;

import java.time.LocalDate;

public record SleepMealResponse(
        LocalDate journalDate,
        Float sleepHour,
        SleepQuality sleepQuality,
        Boolean ateBreakfast,
        Boolean ateLunch,
        Boolean ateDinner
) {
    public static SleepMealResponse from(DailyStatusLog log) {
        return new SleepMealResponse(
                log.getDate(),
                log.getSleepHour(),
                log.getSleepQuality(),
                log.getAteBreakfast(),
                log.getAteLunch(),
                log.getAteDinner()
        );
    }
}
