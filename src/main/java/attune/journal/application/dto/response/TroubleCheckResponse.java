package attune.journal.application.dto.response;

import attune.journal.domain.model.TroubleLog;
import attune.journal.domain.model.TroubleTag;
import attune.journal.domain.model.TroubleType;

import java.time.LocalDateTime;

public record TroubleCheckResponse(
        Long tagId,
        String trouble,
        TroubleType type,
        LocalDateTime checkedAt
) {
    public static TroubleCheckResponse of(TroubleTag tag, TroubleLog log) {
        return new TroubleCheckResponse(tag.getId(), tag.getTrouble(), tag.getType(), log.getCheckedAt());
    }
}
