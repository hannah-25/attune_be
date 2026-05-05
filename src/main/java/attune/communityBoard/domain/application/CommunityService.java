package attune.communityBoard.domain.application;

import attune.common.util.SecurityUtils;
import attune.communityBoard.domain.application.dto.request.CreatePostRequest;
import attune.communityBoard.domain.application.dto.request.UpdatePostRequest;
import attune.communityBoard.domain.application.dto.response.PostResponse;
import attune.user.domain.model.User;
import attune.communityBoard.domain.model.CommunityBoard;
import attune.communityBoard.domain.repository.CommunityBoardRepository;
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
public class CommunityService {

    private final UserRepository userRepository;
    private final CommunityBoardRepository communityBoardRepository;


    public PostResponse createPost(CreatePostRequest createPostRequest){

        UUID userId = SecurityUtils.getCurrentUserUuid();
        if (userId == null) {
            throw new AccessDeniedException("인증이 필요합니다.");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found"));

        LocalDateTime now = LocalDateTime.now();

        CommunityBoard communityBoard = CommunityBoard.builder()
                .user(user)
                .title(createPostRequest.title())
                .content(createPostRequest.content())
                .postCategory(createPostRequest.postCategory())
                .isAnonymous(createPostRequest.isAnonymous())
                .isDeleted(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        CommunityBoard saved = communityBoardRepository.save(communityBoard);
        return PostResponse.from(saved, userId);
    }

    @Transactional(readOnly = true)
    public PostResponse getPost(Long postId) {
        UUID currentUserId = SecurityUtils.getCurrentUserUuid();
        CommunityBoard board = communityBoardRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다."));
        return PostResponse.from(board, currentUserId);
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getPosts() {
        UUID currentUserId = SecurityUtils.getCurrentUserUuid();
        return communityBoardRepository.findAllByIsDeletedFalseOrderByCreatedAtDesc()
                .stream()
                .map(board -> PostResponse.from(board, currentUserId))
                .toList();
    }

    @Transactional
    public PostResponse updatePost(Long postId, UpdatePostRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        CommunityBoard board = communityBoardRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다."));

        if (!board.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("수정 권한이 없습니다.");
        }

        board.update(request.title(), request.content(), request.postCategory());
        return PostResponse.from(board, userId);
    }

    @Transactional
    public void deletePost(Long postId) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        CommunityBoard board = communityBoardRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다."));

        if (!board.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("삭제 권한이 없습니다.");
        }

        board.delete();
    }

}
