package attune.user.adapter.web;

import attune.common.security.CustomUserDetails;
import attune.user.application.UserAccountService;
import attune.user.application.dto.request.ChangePasswordRequest;
import attune.user.application.dto.request.CreateUserRequest;
import attune.user.application.dto.response.CreateUserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@Tag(name = "UserAccount", description = "유저 계정 API")
@RestController
@RequestMapping("/api/users/account")
@RequiredArgsConstructor
public class UserAccountController {

    private final UserAccountService userAccountService;

    @Operation(summary = "회원가입", description = "이메일/비밀번호로 회원가입합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "409", description = "이미 사용 중인 이메일")
    })
    @PostMapping("/signup")
    public ResponseEntity<CreateUserResponse> signup(
            @Valid @RequestBody CreateUserRequest request
    ) {
        return ResponseEntity.ok(userAccountService.signup(request));
    }

    @Operation(summary = "비밀번호 변경", description = "현재 비밀번호를 확인 후 새 비밀번호로 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "변경 성공"),
            @ApiResponse(responseCode = "400", description = "현재 비밀번호 불일치")
    })
    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userAccountService.changePassword(userDetails.getId(), request);
        return ResponseEntity.noContent().build();
    }
}