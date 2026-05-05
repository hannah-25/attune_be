package attune.communityBoard.domain.application.dto.request;

public record CreateCommentRequest(
        String content,
        boolean isAnonymous
) {
}
