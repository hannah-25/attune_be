package attune.medicationAnalysis.adapter.web;

import attune.common.ApiVersion;
import attune.common.util.SecurityUtils;
import attune.medicationAnalysis.application.MedicationAnalysisService;
import attune.medicationAnalysis.application.dto.request.CreateReportRequest;
import attune.medicationAnalysis.application.dto.response.AdherenceSummaryResponse;
import attune.medicationAnalysis.application.dto.response.AvailabilityResponse;
import attune.medicationAnalysis.application.dto.response.ReportDetailResponse;
import attune.medicationAnalysis.application.dto.response.ReportListItemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "Medication Analysis", description = "약물 치료 경과 리포트 API")
@RestController
@RequestMapping(ApiVersion.V1 + "/medication-analysis")
@RequiredArgsConstructor
public class MedicationAnalysisController {

    private final MedicationAnalysisService medicationAnalysisService;

    @Operation(summary = "리포트 생성 가능 여부 확인", description = "선택 기간의 기록 수와 리포트 생성 가능 여부를 반환합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/availability")
    public ResponseEntity<AvailabilityResponse> checkAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(
                medicationAnalysisService.checkAvailability(SecurityUtils.getCurrentUserUuid(), startDate, endDate));
    }

    @Operation(summary = "복용 통계 요약 조회", description = "리포트 생성 없이 복용 기록률·여부 기록률만 반환합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/summary")
    public ResponseEntity<AdherenceSummaryResponse> getSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(
                medicationAnalysisService.getSummary(SecurityUtils.getCurrentUserUuid(), startDate, endDate));
    }

    @Operation(summary = "리포트 생성", description = "선택 기간의 약물 치료 경과 리포트를 생성합니다.")
    @ApiResponse(responseCode = "201", description = "리포트 생성 성공")
    @PostMapping("/reports")
    public ResponseEntity<ReportDetailResponse> createReport(@Valid @RequestBody CreateReportRequest request) {
        ReportDetailResponse response = medicationAnalysisService.createReport(
                SecurityUtils.getCurrentUserUuid(), request);
        return ResponseEntity.created(URI.create(ApiVersion.V1 + "/medication-analysis/reports/" + response.reportId()))
                .body(response);
    }

    @Operation(summary = "리포트 단건 조회")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/reports/{reportId}")
    public ResponseEntity<ReportDetailResponse> getReport(@PathVariable Long reportId) {
        return ResponseEntity.ok(
                medicationAnalysisService.getReport(SecurityUtils.getCurrentUserUuid(), reportId));
    }

    @Operation(summary = "리포트 목록 조회")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/reports")
    public ResponseEntity<List<ReportListItemResponse>> listReports() {
        return ResponseEntity.ok(
                medicationAnalysisService.listReports(SecurityUtils.getCurrentUserUuid()));
    }
}
