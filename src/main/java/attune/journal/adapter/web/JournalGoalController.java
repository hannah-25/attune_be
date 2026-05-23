package attune.journal.adapter.web;

import attune.common.ApiVersion;

import attune.journal.application.DailyGoalService;
import attune.journal.application.dto.request.CreateGoalRequest;
import attune.journal.application.dto.request.ScoreGoalRequest;
import attune.journal.application.dto.request.UpdateGoalRequest;
import attune.journal.application.dto.response.CreateGoalResponse;
import attune.journal.application.dto.response.ScoreGoalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "일지 - 목표 성취도", description = "일일 목표 및 점수 기록 API")
@RequiredArgsConstructor
@RestController
@RequestMapping(ApiVersion.V1 + "/journals")
public class JournalGoalController {

    private final DailyGoalService dailyGoalService;

    @Operation(summary = "목표 성취도 추가", description = "새로운 일일 목표를 등록한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping("/goals")
    public ResponseEntity<CreateGoalResponse> createGoal(@Valid @RequestBody CreateGoalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dailyGoalService.createGoal(request));
    }

    @Operation(summary = "목표 성취도 삭제", description = "journalDate부터 이후의 점수 로그가 함께 삭제된다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "목표 없음")
    })
    @DeleteMapping("/goals/{goalId}")
    public ResponseEntity<Void> deleteGoal(
            @PathVariable Long goalId,
            @Parameter(description = "삭제 시작 날짜 (이 날짜부터 이후의 점수 로그가 삭제된다)", example = "2026-05-09", required = true)
            @NotNull @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate journalDate
    ) {
        dailyGoalService.deleteGoal(goalId, journalDate);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "목표 내용 수정")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "404", description = "목표 없음"),
            @ApiResponse(responseCode = "409", description = "동일한 목표 내용 이미 존재")
    })
    @PatchMapping("/goals/{goalId}")
    public ResponseEntity<CreateGoalResponse> updateGoal(
            @PathVariable Long goalId,
            @Valid @RequestBody UpdateGoalRequest request
    ) {
        return ResponseEntity.ok(dailyGoalService.updateGoal(goalId, request));
    }

    @Operation(summary = "목표 성취도 점수 체크", description = "특정 날짜의 목표 점수를 등록 또는 갱신한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "점수 기록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "404", description = "목표 없음")
    })
    @PostMapping("/{date}/goals")
    public ResponseEntity<ScoreGoalResponse> scoreGoal(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody ScoreGoalRequest request
    ) {
        return ResponseEntity.ok(dailyGoalService.scoreGoal(date, request));
    }
}
