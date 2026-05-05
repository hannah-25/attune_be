package attune.communityBoard.domain.application.dto.response;

import attune.communityBoard.domain.model.Comment;

import java.time.LocalDateTime;

public record UpdateCommentResponse(
        Long commentId,
        LocalDateTime updatedAt
) {
    public static UpdateCommentResponse from(Comment comment) {
        return new UpdateCommentResponse(comment.getId(), comment.getUpdatedAt());
    }
}
