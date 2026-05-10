package attune.notice.adapter.web;

import attune.notice.application.NoticeService;
import attune.notice.application.dto.response.NoticeDetailResponse;
import attune.notice.application.dto.response.NoticeListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "공지사항", description = "공지사항 조회 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "공지사항 목록 조회", description = "고정 공지를 우선으로 최신순 정렬. q로 제목/내용 검색 가능.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    public ResponseEntity<NoticeListResponse> getNotices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String q
    ) {
        return ResponseEntity.ok(noticeService.getNotices(page, size, q));
    }

    @Operation(summary = "공지사항 상세 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "공지사항 없음")
    })
    @GetMapping("/{noticeId}")
    public ResponseEntity<NoticeDetailResponse> getNotice(@PathVariable Long noticeId) {
        return ResponseEntity.ok(noticeService.getNotice(noticeId));
    }
}
