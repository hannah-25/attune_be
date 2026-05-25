package attune.onboarding.adapter.web;

import attune.common.ApiVersion;

import attune.common.security.CustomUserDetails;
import attune.onboarding.application.OnboardingService;
import attune.onboarding.application.dto.request.AsrsRequest;
import attune.onboarding.application.dto.request.GoalRequest;
import attune.onboarding.application.dto.request.SymptomRequest;
import attune.onboarding.application.dto.response.AsrsResponse;
import attune.onboarding.application.dto.response.CompleteOnboardingResponse;
import attune.onboarding.application.dto.response.GoalResponse;
import attune.onboarding.application.dto.response.OnboardingStatusResponse;
import attune.onboarding.application.dto.response.SymptomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Onboarding", description = "온보딩 API")
@RestController
@RequestMapping(ApiVersion.V1 + "/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @Operation(summary = "온보딩 상태 조회", description = "현재 로그인한 사용자의 온보딩 완료 여부를 반환합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/status")
    public ResponseEntity<OnboardingStatusResponse> getOnboardingStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(onboardingService.getOnboardingStatus(userDetails.getId()));
    }

    @Operation(summary = "ASRS 체크리스트 제출", description = "ADHD 자가 진단 척도(ASRS) 응답을 저장하고 점수를 반환합니다.")
    @ApiResponse(responseCode = "201", description = "저장 성공")
    @PostMapping("/asrs")
    public ResponseEntity<AsrsResponse> submitAsrs(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AsrsRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(onboardingService.saveAsrs(userDetails.getId(), request));
    }

    @Operation(summary = "초기 증상 서술 저장", description = "사용자가 직접 기술한 초기 증상을 저장합니다.")
    @ApiResponse(responseCode = "201", description = "저장 성공")
    @PostMapping("/symptoms")
    public ResponseEntity<SymptomResponse> submitSymptom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SymptomRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(onboardingService.saveSymptom(userDetails.getId(), request));
    }

    @Operation(summary = "치료 목표 저장", description = "치료 기대치 및 목표를 저장합니다.")
    @ApiResponse(responseCode = "201", description = "저장 성공")
    @PostMapping("/goals")
    public ResponseEntity<GoalResponse> submitGoals(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody GoalRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(onboardingService.saveGoals(userDetails.getId(), request));
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
