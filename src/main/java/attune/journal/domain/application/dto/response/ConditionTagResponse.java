package attune.journal.domain.application.dto.response;

import attune.journal.domain.model.ConditionTag;

public record ConditionTagResponse(
        Long tagId,
        String condition,
        String conditionType
) {
    public static ConditionTagResponse from(ConditionTag tag) {
        return new ConditionTagResponse(tag.getId(), tag.getCondition(), tag.getConditionType());
    }
}
