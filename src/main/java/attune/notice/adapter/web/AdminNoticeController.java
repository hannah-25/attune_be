package attune.notice.adapter.web;

import attune.common.ApiVersion;

import attune.notice.application.NoticeService;
import attune.notice.application.dto.request.CreateNoticeRequest;
import attune.notice.application.dto.request.UpdateNoticeRequest;
import attune.notice.application.dto.response.CreateNoticeResponse;
import attune.notice.application.dto.response.UpdateNoticeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "관리자 공지사항", description = "관리자 공지사항 관리 API")
@RequiredArgsConstructor
@RestController
@RequestMapping(ApiVersion.V1 + "/admin/notices")
public class AdminNoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "공지사항 등록")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요")
    })
    @PostMapping
    public ResponseEntity<CreateNoticeResponse> createNotice(@Valid @RequestBody CreateNoticeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(noticeService.createNotice(request));
    }

    @Operation(summary = "공지사항 수정")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
            @ApiResponse(responseCode = "404", description = "공지사항 없음")
    })
    @PatchMapping("/{noticeId}")
    public ResponseEntity<UpdateNoticeResponse> updateNotice(@PathVariable Long noticeId,
                                                             @RequestBody UpdateNoticeRequest request) {
        return ResponseEntity.ok(noticeService.updateNotice(noticeId, request));
    }

    @Operation(summary = "공지사항 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
            @ApiResponse(responseCode = "404", description = "공지사항 없음")
    })
    @DeleteMapping("/{noticeId}")
    public ResponseEntity<Void> deleteNotice(@PathVariable Long noticeId) {
        noticeService.deleteNotice(noticeId);
        return ResponseEntity.noContent().build();
    }
}
