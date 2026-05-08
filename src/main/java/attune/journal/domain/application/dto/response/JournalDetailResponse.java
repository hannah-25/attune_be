package attune.journal.domain.application.dto.response;

public record JournalDetailResponse(
        ActiveTagsResponse activeTags,
        CheckedResponse checked
) {}
