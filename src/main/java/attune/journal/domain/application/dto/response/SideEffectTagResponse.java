package attune.journal.domain.application.dto.response;

import attune.journal.domain.model.SideEffectTag;

public record SideEffectTagResponse(
        Long tagId,
        String sideEffect
) {
    public static SideEffectTagResponse from(SideEffectTag tag) {
        return new SideEffectTagResponse(tag.getId(), tag.getSideEffect());
    }
}
