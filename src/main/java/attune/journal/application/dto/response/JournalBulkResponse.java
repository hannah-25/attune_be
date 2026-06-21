package attune.journal.application.dto.response;

import java.util.List;

public record JournalBulkResponse(List<JournalDateResponse> journals) {
}
