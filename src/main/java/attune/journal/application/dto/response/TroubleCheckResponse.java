package attune.journal.application.dto.response;

import attune.journal.domain.model.TroubleLog;
import attune.journal.domain.model.TroubleTag;
import attune.journal.domain.model.TroubleType;

public record TroubleCheckResponse(
        Long tagId,
        String trouble,
        TroubleType type
) {
    public static TroubleCheckResponse of(TroubleTag tag, TroubleLog log) {
        return new TroubleCheckResponse(tag.getId(), tag.getTrouble(), tag.getType());
    }
}
