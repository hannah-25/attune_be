package attune.communityBoard.application.event;

import java.util.UUID;

public record CommentCreatedEvent(
        Long commentId,
        Long postId,
        String postTitle,
        UUID postAuthorId,
        UUID commentAuthorId
) {}
