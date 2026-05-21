package attune.journal.adapter.web;

import attune.journal.application.DailyStatusLogService;
import attune.journal.application.dto.request.CreateDailyStatusRequest;
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

    @Operation(summary = "수면/식사 조회", description = "해당 날짜의 수면/식사 기록을 반환한다. 기록이 없으면 204.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "204", description = "기록 없음")
    })
    @GetMapping
    public ResponseEntity<DailyStatusResponse> get(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return dailyStatusLogService.get(date)
                .map(body -> ResponseEntity.ok(body))
                .orElse(ResponseEntity.noContent().build());
    }

    @Operation(summary = "수면/식사 등록/수정", description = "해당 날짜의 수면/식사 기록을 등록하거나, 이미 존재하면 덮어쓴다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록/수정 성공")
    })
    @PostMapping
    public ResponseEntity<DailyStatusResponse> create(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody CreateDailyStatusRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dailyStatusLogService.create(date, request));
    }
}
