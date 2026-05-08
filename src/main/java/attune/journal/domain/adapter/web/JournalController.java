package attune.journal.domain.adapter.web;

import attune.journal.domain.application.JournalService;
import attune.journal.domain.application.dto.response.DeleteJournalRangeResponse;
import attune.journal.domain.application.dto.response.DeleteJournalResponse;
import attune.journal.domain.application.dto.response.JournalDetailResponse;
import attune.journal.domain.application.dto.response.JournalListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "일지", description = "일지(Journal) 조회/삭제 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/journals")
public class JournalController {

    private final JournalService journalService;

    @Operation(summary = "단일 일지 상세 조회", description = "해당 날짜의 활성 태그와 체크 내역을 반환한다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{date}")
    public ResponseEntity<JournalDetailResponse> getJournal(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(journalService.getJournal(date));
    }

    @Operation(summary = "기간별 일지 목록 조회", description = "기간 내 일지 데이터가 존재하는 날짜 목록을 반환한다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    public ResponseEntity<JournalListResponse> getJournalDates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(journalService.getJournalDates(startDate, endDate));
    }

    @Operation(summary = "단일 일지 삭제", description = "해당 날짜의 모든 일지 기록을 삭제한다.")
    @ApiResponse(responseCode = "200", description = "삭제 성공")
    @DeleteMapping("/{date}")
    public ResponseEntity<DeleteJournalResponse> deleteJournal(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(journalService.deleteJournal(date));
    }

    @Operation(summary = "기간별 일지 삭제", description = "기간 내 모든 일지 기록을 삭제한다.")
    @ApiResponse(responseCode = "200", description = "삭제 성공")
    @DeleteMapping
    public ResponseEntity<DeleteJournalRangeResponse> deleteJournalRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(journalService.deleteJournalRange(startDate, endDate));
    }
}
