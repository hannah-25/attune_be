package attune.journal.application.dto.response;

import attune.journal.domain.model.SideEffectTag;

public record SideEffectTagResponse(
        Long tagId,
        String sideEffect,
        boolean visible
) {
    public static SideEffectTagResponse from(JournalTagResponse r) {
        return new SideEffectTagResponse(r.tagId(), r.name(), r.visible());
    }
}
