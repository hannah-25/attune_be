package attune.user.adapter.web;

import attune.common.security.CustomUserDetails;
import attune.user.application.AccountService;
import attune.user.application.dto.request.ChangePasswordRequest;
import attune.user.application.dto.request.CreateUserRequest;
import attune.user.application.dto.request.PasswordResetConfirmRequest;
import attune.user.application.dto.request.PasswordResetRequest;
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


@Tag(name = "Account", description = "계정 API")
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @Operation(summary = "회원가입", description = "이메일/비밀번호로 회원가입합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "409", description = "이미 사용 중인 이메일")
    })
    @PostMapping("/signup")
    public ResponseEntity<CreateUserResponse> signup(
            @Valid @RequestBody CreateUserRequest request
    ) {
        return ResponseEntity.ok(accountService.signup(request));
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
        accountService.changePassword(userDetails.getId(), request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "비밀번호 재설정 요청", description = "이메일로 비밀번호 재설정 링크를 발송합니다. 존재하지 않는 이메일이어도 동일한 응답을 반환합니다.")
    @ApiResponse(responseCode = "200", description = "이메일 발송 완료")
    @PostMapping("/password/reset")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        accountService.requestPasswordReset(request.email());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "재설정 토큰 검증", description = "이메일로 받은 토큰의 유효성을 확인합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "유효한 토큰"),
            @ApiResponse(responseCode = "400", description = "만료되거나 유효하지 않은 토큰")
    })
    @GetMapping("/password/reset/{token}")
    public ResponseEntity<Void> validateResetToken(@PathVariable String token) {
        accountService.validateResetToken(token);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "비밀번호 재설정 완료", description = "토큰 검증 후 새 비밀번호로 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
            @ApiResponse(responseCode = "400", description = "만료되거나 유효하지 않은 토큰")
    })
    @PostMapping("/password/reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        accountService.confirmPasswordReset(request);
        return ResponseEntity.ok().build();
    }
}