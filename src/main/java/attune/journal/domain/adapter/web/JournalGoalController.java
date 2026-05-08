package attune.journal.domain.adapter.web;

import attune.journal.domain.application.DailyGoalService;
import attune.journal.domain.application.dto.request.CreateGoalRequest;
import attune.journal.domain.application.dto.request.DeleteGoalRequest;
import attune.journal.domain.application.dto.request.ScoreGoalRequest;
import attune.journal.domain.application.dto.response.CreateGoalResponse;
import attune.journal.domain.application.dto.response.ScoreGoalResponse;
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

@Tag(name = "일지 - 목표 성취도", description = "일일 목표 및 점수 기록 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/journals")
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
            @Valid @RequestBody DeleteGoalRequest request
    ) {
        dailyGoalService.deleteGoal(goalId, request);
        return ResponseEntity.noContent().build();
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
