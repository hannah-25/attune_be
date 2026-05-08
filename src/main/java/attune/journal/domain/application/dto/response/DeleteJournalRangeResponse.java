package attune.journal.domain.application.dto.response;

import java.time.LocalDate;

public record DeleteJournalRangeResponse(
        DeletedRange deletedRange,
        int count
) {
    public record DeletedRange(LocalDate startDate, LocalDate endDate) {}

    public static DeleteJournalRangeResponse of(LocalDate startDate, LocalDate endDate, int count) {
        return new DeleteJournalRangeResponse(new DeletedRange(startDate, endDate), count);
    }
}
