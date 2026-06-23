package attune.journal.application.dto.response;

import attune.journal.domain.model.ConditionType;
import attune.journal.domain.repository.JournalTagLogView;

import java.time.LocalDateTime;

public record ConditionCheckResponse(
        Long tagId,
        String condition,
        ConditionType conditionType,
        LocalDateTime checkedAt
) {
    public static ConditionCheckResponse of(JournalTagLogView v) {
        return new ConditionCheckResponse(
                v.tag().getId(),
                v.tag().getName(),
                parseConditionType(v.tag().getTagType()),
                v.log().getCheckedAt()
        );
    }

    private static ConditionType parseConditionType(String tagType) {
        try {
            return ConditionType.valueOf(tagType);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Unrecognized condition tag type stored in DB: " + tagType);
        }
    }
}
