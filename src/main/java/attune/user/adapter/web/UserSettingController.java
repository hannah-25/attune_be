package attune.user.adapter.web;

import attune.common.security.CustomUserDetails;
import attune.user.application.UserSettingService;
import attune.user.application.dto.request.UpdateUserSettingRequest;
import attune.user.application.dto.response.UserSettingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "UserSetting", description = "유저 설정 API")
@RestController
@RequestMapping("/api/users/settings")
@RequiredArgsConstructor
public class UserSettingController {

    private final UserSettingService userSettingService;

    @Operation(summary = "설정 조회", description = "유저 설정을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ResponseEntity<UserSettingResponse> getSettings(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(userSettingService.getSettings(userDetails.getId()));
    }

    @Operation(summary = "설정 변경", description = "유저 설정을 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공")
    })
    @PatchMapping
    public ResponseEntity<UserSettingResponse> updateSettings(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateUserSettingRequest request
    ) {
        return ResponseEntity.ok(userSettingService.updateSettings(userDetails.getId(), request));
    }
}