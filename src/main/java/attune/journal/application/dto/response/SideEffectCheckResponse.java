package attune.journal.application.dto.response;

import attune.journal.domain.model.SideEffectLog;
import attune.journal.domain.model.SideEffectTag;

public record SideEffectCheckResponse(
        Long tagId,
        String sideEffect
) {
    public static SideEffectCheckResponse of(SideEffectTag tag, SideEffectLog log) {
        return new SideEffectCheckResponse(tag.getId(), tag.getSideEffect());
    }
}
