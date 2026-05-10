package attune.journal.application.dto.response;

import java.util.List;

public record ActiveTagsResponse(
        List<ConditionTagResponse> conditions,
        List<SideEffectTagResponse> sideEffects,
        List<TroubleTagResponse> troubles,
        List<GoalActiveResponse> goals
) {}
