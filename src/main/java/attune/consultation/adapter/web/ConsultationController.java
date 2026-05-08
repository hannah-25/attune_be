package attune.consultation.adapter.web;

import attune.consultation.application.ConsultationService;
import attune.consultation.application.dto.request.CreateConsultationRequest;
import attune.consultation.application.dto.request.UpdateConsultationPreparationRequest;
import attune.consultation.application.dto.request.UpdateConsultationResultRequest;
import attune.consultation.application.dto.request.UpdateConsultationScheduleRequest;
import attune.consultation.application.dto.response.ConsultationListResponse;
import attune.consultation.application.dto.response.ConsultationRecordResponse;
import attune.consultation.application.dto.response.ConsultationScheduleResponse;
import attune.consultation.application.dto.response.ConsultationUpdateResponse;
import attune.consultation.application.dto.response.CreateConsultationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "상담", description = "상담 일정 및 기록 관리 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/consultations")
public class ConsultationController {

    private final ConsultationService consultationService;

    @Operation(summary = "상담 일정 등록")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @PostMapping
    public ResponseEntity<CreateConsultationResponse> createConsultation(
            @Valid @RequestBody CreateConsultationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(consultationService.createConsultation(request));
    }

    @Operation(summary = "상담 일정 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "삭제 권한 없음"),
            @ApiResponse(responseCode = "404", description = "상담 일정 없음")
    })
    @DeleteMapping("/{consultationId}")
    public ResponseEntity<Void> deleteConsultation(@PathVariable Long consultationId) {
        consultationService.deleteConsultation(consultationId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "상담 기록 일지 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "삭제 권한 없음"),
            @ApiResponse(responseCode = "404", description = "상담 일정 없음")
    })
    @DeleteMapping("/{consultationId}/result")
    public ResponseEntity<Void> deleteResult(@PathVariable Long consultationId) {
        consultationService.deleteResult(consultationId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "상담 기록 일지 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "상담 일정 없음")
    })
    @GetMapping("/{consultationId}")
    public ResponseEntity<ConsultationRecordResponse> getRecord(@PathVariable Long consultationId) {
        return ResponseEntity.ok(consultationService.getRecord(consultationId));
    }

    @Operation(summary = "상담 전 준비도구 작성/수정")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "작성/수정 성공"),
            @ApiResponse(responseCode = "404", description = "상담 일정 없음")
    })
    @PatchMapping("/{consultationId}/preparation")
    public ResponseEntity<ConsultationUpdateResponse> updatePreparation(
            @PathVariable Long consultationId,
            @Valid @RequestBody UpdateConsultationPreparationRequest request) {
        return ResponseEntity.ok(consultationService.updatePreparation(consultationId, request));
    }

    @Operation(summary = "상담 후 결과 작성/수정")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "작성/수정 성공"),
            @ApiResponse(responseCode = "404", description = "상담 일정 없음")
    })
    @PatchMapping("/{consultationId}/result")
    public ResponseEntity<ConsultationUpdateResponse> updateResult(
            @PathVariable Long consultationId,
            @Valid @RequestBody UpdateConsultationResultRequest request) {
        return ResponseEntity.ok(consultationService.updateResult(consultationId, request));
    }

    @Operation(summary = "상담 이력 기간별 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 조회 기간")
    })
    @GetMapping
    public ResponseEntity<ConsultationListResponse> getConsultations(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(consultationService.getConsultations(startDate, endDate));
    }

    @Operation(summary = "상담 일정 수정")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "404", description = "상담 일정 없음")
    })
    @PatchMapping("/{consultationId}")
    public ResponseEntity<ConsultationScheduleResponse> updateSchedule(
            @PathVariable Long consultationId,
            @Valid @RequestBody UpdateConsultationScheduleRequest request) {
        return ResponseEntity.ok(consultationService.updateSchedule(consultationId, request));
    }
}
