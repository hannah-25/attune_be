package attune.journal.application.dto.response;

import attune.journal.domain.model.ConditionType;

public record ConditionTagResponse(
        Long tagId,
        String condition,
        ConditionType conditionType,
        boolean visible
) {
    public static ConditionTagResponse from(JournalTagResponse r) {
        return new ConditionTagResponse(r.tagId(), r.name(), parseConditionType(r.tagType()), r.visible());
    }

    private static ConditionType parseConditionType(String tagType) {
        try {
            return ConditionType.valueOf(tagType);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Unrecognized condition tag type stored in DB: " + tagType);
        }
    }
}
