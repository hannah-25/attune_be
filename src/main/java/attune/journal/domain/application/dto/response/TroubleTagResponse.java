package attune.journal.domain.application.dto.response;

import attune.journal.domain.model.TroubleTag;

public record TroubleTagResponse(
        Long tagId,
        String trouble,
        String type
) {
    public static TroubleTagResponse from(TroubleTag tag) {
        return new TroubleTagResponse(tag.getId(), tag.getTrouble(), tag.getType());
    }
}
