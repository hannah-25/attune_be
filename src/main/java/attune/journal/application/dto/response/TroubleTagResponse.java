package attune.journal.application.dto.response;

import attune.journal.domain.model.TroubleType;

public record TroubleTagResponse(
        Long tagId,
        String trouble,
        TroubleType type,
        boolean visible
) {
    public static TroubleTagResponse from(JournalTagResponse r) {
        return new TroubleTagResponse(r.tagId(), r.name(), parseTroubleType(r.tagType()), r.visible());
    }

    private static TroubleType parseTroubleType(String tagType) {
        try {
            return TroubleType.valueOf(tagType);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Unrecognized trouble tag type stored in DB: " + tagType);
        }
    }
}
