package attune.journal.application.dto.response;

import attune.journal.domain.model.Memo;

import java.time.LocalDate;

public record MemoResponse(
        LocalDate journalDate,
        String memo
) {
    public static MemoResponse from(Memo memo) {
        return new MemoResponse(
                memo.getJournalDate(),
                memo.getMemo()
        );
    }
}
