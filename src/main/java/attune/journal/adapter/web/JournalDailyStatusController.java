package attune.journal.adapter.web;

import attune.journal.application.DailyStatusLogService;
import attune.journal.application.dto.request.CreateDailyStatusRequest;
import attune.journal.application.dto.request.UpdateDailyStatusRequest;
import attune.journal.application.dto.response.DailyStatusResponse;
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

@Tag(name = "일지 - 수면/식사", description = "수면/식사 기록 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/journals/{date}/sleep-meal")
public class JournalDailyStatusController {

    private final DailyStatusLogService dailyStatusLogService;

    @Operation(summary = "수면/식사 체크", description = "해당 날짜의 수면/식사 기록을 최초 등록한다. 이미 존재하면 409.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "409", description = "해당 날짜 기록 이미 존재")
    })
    @PostMapping
    public ResponseEntity<DailyStatusResponse> create(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody CreateDailyStatusRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dailyStatusLogService.create(date, request));
    }

    @Operation(summary = "수면/식사 정보 수정")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "404", description = "기록 없음")
    })
    @PatchMapping
    public ResponseEntity<DailyStatusResponse> update(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody UpdateDailyStatusRequest request
    ) {
        return ResponseEntity.ok(dailyStatusLogService.update(date, request));
    }
}
