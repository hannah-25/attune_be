package attune.communityBoard.application.event;

import java.util.UUID;

public record CommentCreatedEvent(
        Long postId,
        String postTitle,
        UUID postAuthorId,
        UUID commentAuthorId
) {}
