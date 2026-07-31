package attune.journal.application.dto.response;

import java.util.List;

public record JournalBulkResponse(
        ActiveTagsResponse activeTags,
        List<JournalDateResponse> journals
) {
}
