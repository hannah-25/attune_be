package attune.journal.application.dto.response;

public record JournalDetailResponse(
        ActiveTagsResponse activeTags,
        CheckedResponse checked
) {}
