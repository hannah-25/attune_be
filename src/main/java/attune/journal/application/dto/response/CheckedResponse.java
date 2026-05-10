package attune.journal.application.dto.response;

import java.util.List;

public record CheckedResponse(
        List<ConditionCheckResponse> conditions,
        List<SideEffectCheckResponse> sideEffects,
        List<TroubleCheckResponse> troubles,
        SleepResponse sleep,
        MealResponse meal,
        List<GoalCheckResponse> goals,
        String memo
) {}
