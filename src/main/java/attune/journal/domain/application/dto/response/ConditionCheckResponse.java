package attune.journal.domain.application.dto.response;

import attune.journal.domain.model.ConditionLog;
import attune.journal.domain.model.ConditionTag;

import java.time.LocalDateTime;

public record ConditionCheckResponse(
        Long tagId,
        String condition,
        String conditionType,
        LocalDateTime checkedAt
) {
    public static ConditionCheckResponse of(ConditionTag tag, ConditionLog log) {
        return new ConditionCheckResponse(
                tag.getId(),
                tag.getCondition(),
                tag.getConditionType(),
                log.getCheckedAt()
        );
    }
}
