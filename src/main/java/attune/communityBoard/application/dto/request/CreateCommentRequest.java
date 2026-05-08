package attune.communityBoard.application.dto.request;

public record CreateCommentRequest(
        String content,
        boolean isAnonymous
) {
}
