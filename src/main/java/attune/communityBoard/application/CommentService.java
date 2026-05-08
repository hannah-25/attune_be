package attune.communityBoard.application;

import attune.common.util.SecurityUtils;
import attune.communityBoard.application.dto.request.CreateCommentRequest;
import attune.communityBoard.application.dto.request.UpdateCommentRequest;
import attune.communityBoard.application.dto.response.CommentResponse;
import attune.communityBoard.application.dto.response.CreateCommentResponse;
import attune.communityBoard.application.dto.response.UpdateCommentResponse;
import attune.communityBoard.domain.model.Comment;
import attune.communityBoard.domain.model.CommunityBoard;
import attune.communityBoard.domain.repository.CommentRepository;
import attune.communityBoard.domain.repository.CommunityBoardRepository;
import attune.user.domain.model.User;
import attune.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class CommentService {

    private final UserRepository userRepository;
    private final CommunityBoardRepository communityBoardRepository;
    private final CommentRepository commentRepository;

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long postId) {
        UUID currentUserId = SecurityUtils.getCurrentUserUuid();
        CommunityBoard post = communityBoardRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다."));
        UUID postAuthorId = post.getUser().getId();

        return commentRepository.findAllByCommunityBoardIdAndIsDeletedFalseOrderByCreatedAtAsc(postId)
                .stream()
                .map(comment -> CommentResponse.from(comment, postAuthorId, currentUserId))
                .toList();
    }

    @Transactional
    public CreateCommentResponse createComment(Long postId, CreateCommentRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        if (userId == null) {
            throw new AccessDeniedException("인증이 필요합니다.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        CommunityBoard post = communityBoardRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다."));

        LocalDateTime now = LocalDateTime.now();
        Comment comment = Comment.builder()
                .user(user)
                .communityBoard(post)
                .content(request.content())
                .isAnonymous(request.isAnonymous())
                .isDeleted(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Comment saved = commentRepository.save(comment);
        return CreateCommentResponse.from(saved, post.getUser().getId());
    }

    @Transactional
    public UpdateCommentResponse updateComment(Long commentId, UpdateCommentRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        Comment comment = commentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new NoSuchElementException("댓글을 찾을 수 없습니다."));

        if (!comment.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("수정 권한이 없습니다.");
        }

        comment.update(request.isAnonymous(), request.content());
        return UpdateCommentResponse.from(comment);
    }

    @Transactional
    public void deleteComment(Long commentId) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        Comment comment = commentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new NoSuchElementException("댓글을 찾을 수 없습니다."));

        if (!comment.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("삭제 권한이 없습니다.");
        }

        comment.delete();
    }
}
