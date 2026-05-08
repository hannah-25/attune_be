package attune.journal.domain.application.dto.response;

import attune.journal.domain.model.SideEffectLog;
import attune.journal.domain.model.SideEffectTag;

import java.time.LocalDateTime;

public record SideEffectCheckResponse(
        Long tagId,
        String sideEffect,
        LocalDateTime checkedAt
) {
    public static SideEffectCheckResponse of(SideEffectTag tag, SideEffectLog log) {
        return new SideEffectCheckResponse(tag.getId(), tag.getSideEffect(), log.getCheckedAt());
    }
}
