package attune.journal.application.dto.response;

public record SideEffectTagResponse(
        Long tagId,
        String sideEffect,
        boolean visible
) {
    public static SideEffectTagResponse from(JournalTagResponse r) {
        return new SideEffectTagResponse(r.tagId(), r.name(), r.visible());
    }
}
