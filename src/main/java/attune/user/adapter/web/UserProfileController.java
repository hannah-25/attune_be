package attune.user.adapter.web;

import attune.common.security.CustomUserDetails;
import attune.user.application.AccountService;
import attune.user.application.UserProfileService;
import attune.user.application.dto.request.UpdateNicknameRequest;
import attune.user.application.dto.request.UpdateProfileImageRequest;
import attune.user.application.dto.response.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "UserProfile", description = "유저 프로필 API")
@RestController
@RequestMapping("/v1/users/me")
@RequiredArgsConstructor
public class UserProfileController {

    private final AccountService accountService;
    private final UserProfileService userProfileService;

    @Operation(summary = "프로필 조회", description = "내 프로필 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(userProfileService.getProfile(userDetails.getId()));
    }

    @Operation(summary = "닉네임 수정", description = "로그인한 사용자의 닉네임을 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "변경 성공"),
            @ApiResponse(responseCode = "409", description = "이미 사용 중인 닉네임")
    })
    @PutMapping("/nickname")
    public ResponseEntity<Void> updateNickname(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateNicknameRequest request
    ) {
        accountService.updateNickname(userDetails.getId(), request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "프로필 사진 수정", description = "로그인한 사용자의 프로필 이미지 URL을 변경합니다.")
    @ApiResponse(responseCode = "204", description = "변경 성공")
    @PostMapping("/image")
    public ResponseEntity<Void> updateProfileImage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateProfileImageRequest request
    ) {
        accountService.updateProfileImage(userDetails.getId(), request);
        return ResponseEntity.noContent().build();
    }
}
