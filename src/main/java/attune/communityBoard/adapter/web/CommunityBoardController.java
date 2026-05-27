package attune.communityBoard.adapter.web;

import attune.common.ApiVersion;
import attune.communityBoard.application.CommunityService;
import attune.communityBoard.application.dto.request.CreatePostRequest;
import attune.communityBoard.application.dto.request.UpdatePostRequest;
import attune.communityBoard.application.dto.response.PostResponse;
import attune.communityBoard.domain.model.PostCategory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "커뮤니티 게시판", description = "커뮤니티 게시글 CRUD API")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping(ApiVersion.V1 + "/community")
public class CommunityBoardController {

    private final CommunityService communityService;

    @Operation(summary = "게시글 작성", description = "새로운 게시글을 작성합니다. 익명 여부를 선택할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "게시글 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @PostMapping("/posts")
    public ResponseEntity<PostResponse> createPost(@Valid @RequestBody CreatePostRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(communityService.createPost(request));
    }

    @Operation(
            summary = "게시글 목록 조회 / 검색",
            description = """
                    삭제되지 않은 게시글을 최신순으로 페이지 단위로 반환합니다.
                    - q: 제목 또는 본문에 포함된 키워드 (대소문자 무시). 생략 시 전체 조회.
                    - category: 카테고리 필터 (DEFAULT / DISORDER_INFO / MEDICATION / DAILY_LIFE). 생략 시 전체 카테고리.
                    - page: 페이지 번호 (0부터 시작, 기본값 0)
                    - size: 페이지 크기 (기본값 20)
                    두 파라미터는 AND 조건으로 결합됩니다.
                    """
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/posts")
    public ResponseEntity<Page<PostResponse>> getPosts(
            @Parameter(description = "제목·본문 키워드 검색 (선택)")
            @jakarta.validation.constraints.Size(max = 100, message = "검색어는 100자 이내여야 합니다.")
            @RequestParam(required = false) String q,
            @Parameter(description = "카테고리 필터 (선택): DEFAULT, DISORDER_INFO, MEDICATION, DAILY_LIFE")
            @RequestParam(required = false) PostCategory category,
            @Parameter(description = "페이지 번호 (0부터 시작, 기본값 0)")
            @Min(0) @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (기본값 20, 최대 100)")
            @Min(1) @Max(100) @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(communityService.getPosts(q, category, PageRequest.of(page, size)));
    }

    @Operation(summary = "게시글 상세 조회", description = "게시글 ID로 단건 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "게시글 없음")
    })
    @GetMapping("/posts/{postId}")
    public ResponseEntity<PostResponse> getPost(@PathVariable Long postId) {
        return ResponseEntity.ok(communityService.getPost(postId));
    }

    @Operation(summary = "게시글 수정", description = "작성자 본인만 게시글을 수정할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "403", description = "수정 권한 없음"),
            @ApiResponse(responseCode = "404", description = "게시글 없음")
    })
    @PutMapping("/posts/{postId}")
    public ResponseEntity<PostResponse> updatePost(@PathVariable Long postId,
                                                   @Valid @RequestBody UpdatePostRequest request) {
        return ResponseEntity.ok(communityService.updatePost(postId, request));
    }

    @Operation(summary = "게시글 삭제", description = "작성자 본인만 게시글을 삭제할 수 있습니다. 소프트 삭제로 처리됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "삭제 권한 없음"),
            @ApiResponse(responseCode = "404", description = "게시글 없음")
    })
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId) {
        communityService.deletePost(postId);
        return ResponseEntity.noContent().build();
    }

}
