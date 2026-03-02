package attune.user.adapter.web;

import attune.user.application.UserSettingService;
import attune.user.application.dto.request.*;
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

import java.util.UUID;

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
            @AuthenticationPrincipal UUID userId
    ) {
        return ResponseEntity.ok(userSettingService.getSettings(userId));
    }

    @Operation(summary = "이메일 알림 설정", description = "이메일 알림 설정을 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공")
    })
    @PatchMapping("/email-notification")
    public ResponseEntity<UserSettingResponse> updateEmailNotification(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UpdateEmailNotificationRequest request
    ) {
        return ResponseEntity.ok(userSettingService.updateEmailNotification(userId, request));
    }

    @Operation(summary = "푸시 알림 설정", description = "푸시 알림 설정을 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공")
    })
    @PatchMapping("/push-notification")
    public ResponseEntity<UserSettingResponse> updatePushNotification(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UpdatePushNotificationRequest request
    ) {
        return ResponseEntity.ok(userSettingService.updatePushNotification(userId, request));
    }

    @Operation(summary = "테마 설정", description = "테마를 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공")
    })
    @PatchMapping("/theme")
    public ResponseEntity<UserSettingResponse> updateTheme(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UpdateThemeRequest request
    ) {
        return ResponseEntity.ok(userSettingService.updateTheme(userId, request));
    }

    @Operation(summary = "언어/지역 설정", description = "날짜 형식, 시간 형식, 주 시작일을 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공")
    })
    @PatchMapping("/localization")
    public ResponseEntity<UserSettingResponse> updateLocalization(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UpdateLocalizationRequest request
    ) {
        return ResponseEntity.ok(userSettingService.updateLocalization(userId, request));
    }

    @Operation(summary = "워크스페이스 설정", description = "워크스페이스 관련 설정을 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공")
    })
    @PatchMapping("/workspace")
    public ResponseEntity<UserSettingResponse> updateWorkspaceSetting(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UpdateWorkspaceSettingRequest request
    ) {
        return ResponseEntity.ok(userSettingService.updateWorkspaceSetting(userId, request));
    }
}