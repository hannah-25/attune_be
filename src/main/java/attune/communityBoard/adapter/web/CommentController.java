package attune.communityBoard.adapter.web;

import attune.common.ApiVersion;

import attune.communityBoard.application.CommentService;
import attune.communityBoard.application.dto.request.CreateCommentRequest;
import attune.communityBoard.application.dto.request.UpdateCommentRequest;
import attune.communityBoard.application.dto.response.CommentResponse;
import attune.communityBoard.application.dto.response.CreateCommentResponse;
import attune.communityBoard.application.dto.response.UpdateCommentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "커뮤니티 댓글", description = "커뮤니티 게시글 댓글 API")
@RequiredArgsConstructor
@RestController
@RequestMapping(ApiVersion.V1 + "/community")
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "댓글 목록 조회", description = "게시글의 댓글을 오래된 순으로 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "게시글 없음")
    })
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable Long postId) {
        return ResponseEntity.ok(commentService.getComments(postId));
    }

    @Operation(summary = "댓글 작성", description = "게시글에 댓글을 작성합니다. 익명 여부를 선택할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "댓글 생성 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "게시글 없음")
    })
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<CreateCommentResponse> createComment(@PathVariable Long postId,
                                                               @Valid @RequestBody CreateCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.createComment(postId, request));
    }

    @Operation(summary = "댓글 수정", description = "작성자 본인만 댓글을 수정할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "403", description = "수정 권한 없음"),
            @ApiResponse(responseCode = "404", description = "댓글 없음")
    })
    @PatchMapping("/comments/{commentId}")
    public ResponseEntity<UpdateCommentResponse> updateComment(@PathVariable Long commentId,
                                                               @Valid @RequestBody UpdateCommentRequest request) {
        return ResponseEntity.ok(commentService.updateComment(commentId, request));
    }

    @Operation(summary = "댓글 삭제", description = "작성자 본인만 댓글을 삭제할 수 있습니다. 소프트 삭제로 처리됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "삭제 권한 없음"),
            @ApiResponse(responseCode = "404", description = "댓글 없음")
    })
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }
}
