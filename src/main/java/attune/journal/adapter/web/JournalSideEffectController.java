package attune.journal.adapter.web;

import attune.journal.application.SideEffectTagService;
import attune.journal.application.dto.request.CheckSideEffectRequest;
import attune.journal.application.dto.request.CreateSideEffectTagRequest;
import attune.journal.application.dto.request.DeleteTagRequest;
import attune.journal.application.dto.response.SideEffectCheckResponse;
import attune.journal.application.dto.response.SideEffectTagResponse;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "일지 - 부작용", description = "부작용 태그 및 체크 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/journals")
public class JournalSideEffectController {

    private final SideEffectTagService sideEffectTagService;

    @Operation(summary = "부작용 태그 목록 조회", description = "현재 사용자의 활성 부작용 태그를 모두 반환한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/side-effect-tags")
    public ResponseEntity<List<SideEffectTagResponse>> getTags() {
        return ResponseEntity.ok(sideEffectTagService.getActiveTags());
    }

    @Operation(summary = "부작용 태그 추가")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping("/side-effect-tags")
    public ResponseEntity<SideEffectTagResponse> createTag(@Valid @RequestBody CreateSideEffectTagRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sideEffectTagService.createTag(request));
    }

    @Operation(summary = "부작용 태그 삭제", description = "journalDate부터 이후의 체크 로그가 함께 삭제된다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "태그 없음")
    })
    @DeleteMapping("/side-effect-tags/{tagId}")
    public ResponseEntity<Void> deleteTag(
            @PathVariable Long tagId,
            @Valid @RequestBody DeleteTagRequest request
    ) {
        sideEffectTagService.deleteTag(tagId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "부작용 태그 체크")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "체크 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "404", description = "태그 없음")
    })
    @PostMapping("/side-effects")
    public ResponseEntity<SideEffectCheckResponse> check(@Valid @RequestBody CheckSideEffectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sideEffectTagService.check(request));
    }
}
