package attune.journal.adapter.web;

import attune.journal.application.MemoService;
import attune.journal.application.dto.request.CreateMemoRequest;
import attune.journal.application.dto.request.UpdateMemoRequest;
import attune.journal.application.dto.response.MemoResponse;
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

@Tag(name = "일지 - 메모", description = "일지 메모 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/journals/{date}/memo")
public class JournalMemoController {

    private final MemoService memoService;

    @Operation(summary = "메모 등록", description = "해당 날짜의 메모를 최초 등록한다. 이미 존재하면 409.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "409", description = "해당 날짜 메모 이미 존재")
    })
    @PostMapping
    public ResponseEntity<MemoResponse> create(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody CreateMemoRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(memoService.create(date, request));
    }

    @Operation(summary = "메모 수정")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "404", description = "메모 없음")
    })
    @PatchMapping
    public ResponseEntity<MemoResponse> update(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody UpdateMemoRequest request
    ) {
        return ResponseEntity.ok(memoService.update(date, request));
    }
}
