package attune.medication.domain.adapter.web;

import attune.medication.domain.application.DrugService;
import attune.medication.domain.application.dto.response.DrugDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "의약품 표준 정보", description = "의약품 표준 정보 조회 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/drugs")
public class DrugController {

    private final DrugService drugService;

    @Operation(summary = "의약품 표준 정보 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "의약품 없음")
    })
    @GetMapping("/{standardDrugId}")
    public ResponseEntity<DrugDetailResponse> getDrug(@PathVariable Long standardDrugId) {
        return ResponseEntity.ok(drugService.getDrug(standardDrugId));
    }
}
