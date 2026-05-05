package attune.communityBoard.domain.application.dto.response;

import attune.communityBoard.domain.model.Comment;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateCommentResponse(
        Long commentId,
        String anonNickname,
        boolean isPostAuthor,
        LocalDateTime createdAt
) {
    public static CreateCommentResponse from(Comment comment, UUID postAuthorId) {
        String anonNickname = comment.getIsAnonymous() ? "익명" : comment.getUser().getNickname();
        boolean isPostAuthor = comment.getUser().getId().equals(postAuthorId);
        return new CreateCommentResponse(
                comment.getId(),
                anonNickname,
                isPostAuthor,
                comment.getCreatedAt()
        );
    }
}
