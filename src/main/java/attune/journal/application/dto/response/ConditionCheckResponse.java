package attune.journal.application.dto.response;

import attune.journal.domain.model.ConditionLog;
import attune.journal.domain.model.ConditionTag;
import attune.journal.domain.model.ConditionType;

public record ConditionCheckResponse(
        Long tagId,
        String condition,
        ConditionType conditionType
) {
    public static ConditionCheckResponse of(ConditionTag tag, ConditionLog log) {
        return new ConditionCheckResponse(
                tag.getId(),
                tag.getCondition(),
                tag.getConditionType()
        );
    }
}
