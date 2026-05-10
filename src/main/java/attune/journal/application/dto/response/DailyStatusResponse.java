package attune.journal.application.dto.response;

import attune.journal.domain.model.DailyStatusLog;
import attune.journal.domain.model.SleepQuality;

import java.time.LocalDate;

public record DailyStatusResponse(
        LocalDate journalDate,
        Float sleepHour,
        SleepQuality sleepQuality,
        Boolean ateBreakfast,
        Boolean ateLunch,
        Boolean ateDinner
) {
    public static DailyStatusResponse from(DailyStatusLog log) {
        return new DailyStatusResponse(
                log.getDate(),
                log.getSleepHour(),
                log.getSleepQuality(),
                log.getAteBreakfast(),
                log.getAteLunch(),
                log.getAteDinner()
        );
    }
}
