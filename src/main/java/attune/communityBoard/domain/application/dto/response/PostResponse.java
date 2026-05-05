package attune.communityBoard.domain.application.dto.response;

import attune.communityBoard.domain.model.CommunityBoard;
import attune.communityBoard.domain.model.PostCategory;


import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public record PostResponse(
        Long postId,
        String title,
        String content,
        PostCategory postCategory,
        String anonNickname,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean isOwner
) {
    public static PostResponse from(CommunityBoard board, UUID currentUserId) {
        String anonNickname = board.getIsAnonymous() ? "익명" : board.getUser().getNickname();
        boolean isOwner = Objects.equals(board.getUser().getId(), currentUserId);
        return new PostResponse(
                board.getId(),
                board.getTitle(),
                board.getContent(),
                board.getPostCategory(),
                anonNickname,
                board.getCreatedAt(),
                board.getUpdatedAt(),
                isOwner
        );
    }
}