package attune.medication.domain.adapter.web;

import attune.medication.domain.application.MedicationService;
import attune.medication.domain.application.dto.request.CreateMedicationRequest;
import attune.medication.domain.application.dto.request.UpdateMedicationRequest;
import attune.medication.domain.application.dto.request.UpdateMedicationScheduleRequest;
import attune.medication.domain.application.dto.response.*;
import attune.medication.domain.application.dto.response.MedicationDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;

@Tag(name = "약물 복용", description = "약물 복용 프로필 및 이력 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/medications")
public class MedicationController {

    private final MedicationService medicationService;

    @Operation(summary = "의약품 표준 정보 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "의약품 없음")
    })
    @GetMapping("/standards/{medicationId}")
    public ResponseEntity<MedicationDetailResponse> getMedicationDetail(@PathVariable Long medicationId) {
        return ResponseEntity.ok(medicationService.getMedicationDetail(medicationId));
    }

    @Operation(summary = "복용 중인 약물 프로필 등록")
    @ApiResponse(responseCode = "201", description = "등록 성공")
    @PostMapping
    public ResponseEntity<CreateMedicationResponse> createMedication(
            @Valid @RequestBody CreateMedicationRequest request
    ) {
        CreateMedicationResponse response = medicationService.createMedication(request);
        return ResponseEntity
                .created(URI.create("/api/medications/" + response.medicationId()))
                .body(response);
    }

    @Operation(summary = "약물 정보/복용 상태 수정")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "404", description = "약물 없음")
    })
    @PatchMapping("/{userMedicationId}")
    public ResponseEntity<UpdateMedicationResponse> updateMedication(
            @PathVariable Long userMedicationId,
            @RequestBody UpdateMedicationRequest request
    ) {
        return ResponseEntity.ok(medicationService.updateMedication(medicationId, request));
    }

    @Operation(summary = "특정 약물별 상세 이력 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "약물 없음")
    })
    @GetMapping("/{medicationId}/logs")
    public ResponseEntity<MedicationLogResponse> getMedicationLogs(
            @PathVariable Long medicationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(medicationService.getMedicationLogs(medicationId, startDate, endDate));
    }

    @Operation(summary = "복용 시간 알림 설정")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "설정 성공"),
            @ApiResponse(responseCode = "404", description = "약물 없음")
    })
    @PutMapping("/{medicationId}/schedule")
    public ResponseEntity<UpdateMedicationScheduleResponse> updateSchedule(
            @PathVariable Long medicationId,
            @Valid @RequestBody UpdateMedicationScheduleRequest request
    ) {
        return ResponseEntity.ok(medicationService.updateSchedule(medicationId, request));
    }
}
