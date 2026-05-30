package attune.journal.application.dto.response;

import attune.journal.domain.model.ConditionTag;
import attune.journal.domain.model.ConditionType;

public record ConditionTagResponse(
        Long tagId,
        String condition,
        ConditionType conditionType,
        boolean visible
) {
    public static ConditionTagResponse from(ConditionTag tag) {
        return new ConditionTagResponse(tag.getId(), tag.getCondition(), tag.getConditionType(), tag.isVisible());
    }
}
