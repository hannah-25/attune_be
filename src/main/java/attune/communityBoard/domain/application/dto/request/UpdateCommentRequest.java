package attune.communityBoard.domain.application.dto.request;

public record UpdateCommentRequest(
        String content,
        Boolean isAnonymous
) {
}
