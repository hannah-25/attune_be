package attune.communityBoard.application.dto.request;

public record UpdateCommentRequest(
        String content,
        Boolean isAnonymous
) {
}
