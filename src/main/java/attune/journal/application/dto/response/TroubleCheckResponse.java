package attune.journal.application.dto.response;

import attune.journal.domain.model.TroubleType;
import attune.journal.domain.repository.JournalTagLogView;

import java.time.LocalDateTime;

public record TroubleCheckResponse(
        Long tagId,
        String trouble,
        TroubleType type,
        LocalDateTime checkedAt
) {
    public static TroubleCheckResponse of(JournalTagLogView v) {
        return new TroubleCheckResponse(
                v.tag().getId(),
                v.tag().getName(),
                parseTroubleType(v.tag().getTagType()),
                v.log().getCheckedAt()
        );
    }

    private static TroubleType parseTroubleType(String tagType) {
        try {
            return TroubleType.valueOf(tagType);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Unrecognized trouble tag type stored in DB: " + tagType);
        }
    }
}
