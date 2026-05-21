package attune.journal.application.dto.response;

import attune.journal.domain.model.TroubleTag;
import attune.journal.domain.model.TroubleType;

public record TroubleTagResponse(
        Long tagId,
        String trouble,
        TroubleType type
) {
    public static TroubleTagResponse from(TroubleTag tag) {
        return new TroubleTagResponse(tag.getId(), tag.getTrouble(), tag.getType());
    }
}
