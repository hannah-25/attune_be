package attune.communityBoard.application.dto.response;

import attune.communityBoard.domain.model.Comment;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public record CommentResponse(
        Long commentId,
        String anonNickname,
        String content,
        LocalDateTime createdAt,
        boolean isPostAuthor,
        boolean isOwner
) {
    public static CommentResponse from(Comment comment, UUID postAuthorId, UUID currentUserId) {
        String anonNickname = comment.getIsAnonymous() ? "익명" : comment.getUser().getNickname();
        return new CommentResponse(
                comment.getId(),
                anonNickname,
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUser().getId().equals(postAuthorId),
                Objects.equals(comment.getUser().getId(), currentUserId)
        );
    }
}
