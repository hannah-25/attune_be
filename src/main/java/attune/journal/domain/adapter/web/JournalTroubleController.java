package attune.journal.domain.adapter.web;

import attune.journal.domain.application.TroubleTagService;
import attune.journal.domain.application.dto.request.CheckTroubleRequest;
import attune.journal.domain.application.dto.request.CreateTroubleTagRequest;
import attune.journal.domain.application.dto.request.DeleteTagRequest;
import attune.journal.domain.application.dto.response.TroubleCheckResponse;
import attune.journal.domain.application.dto.response.TroubleTagResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "일지 - 업무 실수/불편", description = "업무적 실수/불편 태그 및 체크 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/journals")
public class JournalTroubleController {

    private final TroubleTagService troubleTagService;

    @Operation(summary = "업무적 실수/불편 태그 추가")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping("/trouble-tags")
    public ResponseEntity<TroubleTagResponse> createTag(@Valid @RequestBody CreateTroubleTagRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(troubleTagService.createTag(request));
    }

    @Operation(summary = "업무적 실수/불편 태그 삭제", description = "journalDate부터 이후의 체크 로그가 함께 삭제된다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "태그 없음")
    })
    @DeleteMapping("/trouble-tags/{tagId}")
    public ResponseEntity<Void> deleteTag(
            @PathVariable Long tagId,
            @Valid @RequestBody DeleteTagRequest request
    ) {
        troubleTagService.deleteTag(tagId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "업무적 실수/불편 발생 체크")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "체크 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "404", description = "태그 없음")
    })
    @PostMapping("/{date}/troubles")
    public ResponseEntity<TroubleCheckResponse> check(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody CheckTroubleRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(troubleTagService.check(date, request));
    }
}
