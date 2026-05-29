package attune.medication.adapter.web;

import attune.common.ApiVersion;
import attune.medication.application.MedicationService;
import attune.medication.application.dto.request.CreateMedicationRequest;
import attune.medication.application.dto.request.QuickLogRequest;
import attune.medication.application.dto.request.UpdateMedicationRequest;
import attune.medication.application.dto.response.CreateMedicationResponse;
import attune.medication.application.dto.response.MedicationDetailResponse;
import attune.medication.application.dto.response.MedicationLogResponse;
import attune.medication.application.dto.response.MedicationPeriodLogResponse;
import attune.medication.application.dto.response.QuickLogResponse;
import attune.medication.application.dto.response.UpdateMedicationResponse;
import attune.medication.application.dto.response.UserMedicationListItemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "Medication", description = "Medication and user-medication APIs")
@RequiredArgsConstructor
@RestController
@RequestMapping(ApiVersion.V1)
public class MedicationController {

    private final MedicationService medicationService;

    @Operation(summary = "Get user medication list")
    @ApiResponse(responseCode = "200", description = "Success")
    @GetMapping("/user-medications")
    public ResponseEntity<List<UserMedicationListItemResponse>> getUserMedications() {
        return ResponseEntity.ok(medicationService.getUserMedications());
    }

    @Operation(summary = "Get medication standard detail")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "404", description = "Medication not found")
    })
    @GetMapping("/medications/standards/{medicationId}")
    public ResponseEntity<MedicationDetailResponse> getMedicationDetail(@PathVariable Long medicationId) {
        return ResponseEntity.ok(medicationService.getMedicationDetail(medicationId));
    }

    @Operation(summary = "Create user medication")
    @ApiResponse(responseCode = "201", description = "Created")
    @PostMapping("/user-medications")
    public ResponseEntity<CreateMedicationResponse> createMedication(
            @Valid @RequestBody CreateMedicationRequest request
    ) {
        CreateMedicationResponse response = medicationService.createMedication(request);
        return ResponseEntity
                .created(URI.create(ApiVersion.V1 + "/user-medications/" + response.userMedicationId()))
                .body(response);
    }

    @Operation(summary = "Update user medication")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "404", description = "User medication not found")
    })
    @PatchMapping("/user-medications/{userMedicationId}")
    public ResponseEntity<UpdateMedicationResponse> updateMedication(
            @PathVariable Long userMedicationId,
            @RequestBody UpdateMedicationRequest request
    ) {
        return ResponseEntity.ok(medicationService.updateMedication(userMedicationId, request));
    }

    @Operation(summary = "Get logs for a user medication")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "404", description = "User medication not found")
    })
    @GetMapping("/user-medications/{userMedicationId}/logs")
    public ResponseEntity<MedicationLogResponse> getMedicationLogs(
            @PathVariable Long userMedicationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(medicationService.getMedicationLogs(userMedicationId, startDate, endDate));
    }

    @Operation(summary = "Get logs for a period")
    @ApiResponse(responseCode = "200", description = "Success")
    @GetMapping("/user-medications/logs")
    public ResponseEntity<MedicationPeriodLogResponse> getMedicationPeriodLogs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(medicationService.getMedicationPeriodLogs(startDate, endDate));
    }

    @Operation(summary = "Quick log action from reminder")
    @ApiResponse(responseCode = "201", description = "Created")
    @PostMapping("/user-medications/{userMedicationId}/log/quick")
    public ResponseEntity<QuickLogResponse> quickLog(
            @PathVariable Long userMedicationId,
            @Valid @RequestBody QuickLogRequest request
    ) {
        return ResponseEntity.status(201).body(medicationService.quickLog(userMedicationId, request));
    }
}
