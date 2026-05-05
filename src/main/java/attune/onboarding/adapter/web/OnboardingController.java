package attune.onboarding.adapter.web;

import attune.common.security.CustomUserDetails;
import attune.onboarding.application.OnboardingService;
import attune.onboarding.application.dto.request.AsrsRequest;
import attune.onboarding.application.dto.request.SymptomRequest;
import attune.onboarding.application.dto.response.AsrsResponse;
import attune.onboarding.application.dto.response.CompleteOnboardingResponse;
import attune.onboarding.application.dto.response.SymptomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Onboarding", description = "온보딩 API")
@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @Operation(summary = "ASRS 체크리스트 제출", description = "ADHD 자가 진단 척도(ASRS) 응답을 저장하고 점수를 반환합니다.")
    @ApiResponse(responseCode = "200", description = "저장 성공")
    @PostMapping("/asrs")
    public ResponseEntity<AsrsResponse> submitAsrs(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AsrsRequest request
    ) {
        return ResponseEntity.ok(onboardingService.saveAsrs(userDetails.getId(), request));
    }

    @Operation(summary = "초기 증상 서술 저장", description = "사용자가 직접 기술한 초기 증상을 저장합니다.")
    @ApiResponse(responseCode = "200", description = "저장 성공")
    @PostMapping("/symptoms")
    public ResponseEntity<SymptomResponse> submitSymptom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SymptomRequest request
    ) {
        return ResponseEntity.ok(onboardingService.saveSymptom(userDetails.getId(), request));
    }

    @Operation(summary = "온보딩 최종 제출", description = "ASRS와 증상 서술이 모두 완료된 경우 온보딩을 완료 처리합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "온보딩 완료"),
            @ApiResponse(responseCode = "400", description = "필수 단계 미완료")
    })
    @PostMapping("/complete")
    public ResponseEntity<CompleteOnboardingResponse> completeOnboarding(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(onboardingService.completeOnboarding(userDetails.getId()));
    }
}
