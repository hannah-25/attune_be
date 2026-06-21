package attune.journal.application.dto.response;

import java.time.LocalDate;

public record JournalDateResponse(
        LocalDate date,
        CheckedResponse checked,
        ActiveTagsResponse activeTags
) {
}
