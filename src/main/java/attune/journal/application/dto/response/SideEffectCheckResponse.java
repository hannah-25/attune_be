package attune.journal.application.dto.response;

import attune.journal.domain.repository.JournalTagLogView;

import java.time.LocalDateTime;

public record SideEffectCheckResponse(
        Long tagId,
        String sideEffect,
        LocalDateTime checkedAt
) {
    public static SideEffectCheckResponse of(JournalTagLogView v) {
        return new SideEffectCheckResponse(v.tag().getId(), v.tag().getName(), v.log().getCheckedAt());
    }
}
