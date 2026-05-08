package attune.journal.domain.application.dto.request;

import attune.journal.domain.model.SleepQuality;

public record UpdateSleepMealRequest(
        Float sleepHour,
        SleepQuality sleepQuality,
        Boolean ateBreakfast,
        Boolean ateLunch,
        Boolean ateDinner
) {}
