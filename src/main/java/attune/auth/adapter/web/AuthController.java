package attune.auth.adapter.web;

import attune.auth.application.AuthService;
import attune.auth.application.dto.request.LoginRequest;
import attune.auth.application.dto.request.RestoreRequest;
import attune.auth.application.dto.response.AuthResult;
import attune.auth.application.dto.response.LoginResponse;
import attune.auth.application.dto.response.RestoreResponse;
import attune.common.config.JwtConfig;
import attune.common.security.CustomUserDetails;
import attune.common.util.CookieUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
// 인증 : 로그인, 로그아웃, 토큰 관리

    private final AuthService authService;
    private final CookieUtil cookieUtil;
    private final JwtConfig jwtConfig;

    @Operation(summary = "로그인", description = "이메일/비밀번호로 로그인합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthResult result = authService.login(request);

        cookieUtil.addCookie(response, "refresh_token", result.refreshToken(),
                "/api/auth", jwtConfig.getRefreshTokenExpiration());

        return ResponseEntity.ok(result.loginResponse());
    }


    @Operation(summary = "토큰 재발급", description = "Refresh Token으로 Access Token을 재발급합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재발급 성공"),
            @ApiResponse(responseCode = "401", description = "리프레시 토큰 만료 또는 유효하지 않음")
    })
    @PostMapping("/reissue")
    public ResponseEntity<LoginResponse> reissue(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieUtil.extractCookie(request, "refresh_token");
        if (refreshToken == null) {
            return ResponseEntity.status(401).build();
        }

        String authHeader = request.getHeader("Authorization");
        String accessToken = (authHeader != null && authHeader.startsWith("Bearer ")) ? authHeader.substring(7) : null;
        AuthResult result = authService.reissue(refreshToken, accessToken);

        cookieUtil.addCookie(response, "refresh_token", result.refreshToken(),
                "/api/auth", jwtConfig.getRefreshTokenExpiration());

        return ResponseEntity.ok(result.loginResponse());
    }


    @Operation(summary = "로그아웃", description = "Refresh Token을 삭제하고 로그아웃합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal CustomUserDetails userDetails, HttpServletResponse response) {
        authService.logout(userDetails.getId());
        cookieUtil.removeCookie(response, "refresh_token", "/api/auth");
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "회원 복구", description = "탈퇴 상태의 계정을 이메일/비밀번호로 인증 후 복구합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "복구 성공"),
            @ApiResponse(responseCode = "400", description = "탈퇴 상태가 아닌 계정"),
            @ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치")
    })
    @PostMapping("/restore")
    public ResponseEntity<RestoreResponse> restore(
            @jakarta.validation.Valid @RequestBody RestoreRequest request,
            HttpServletResponse response
    ) {
        AuthResult result = authService.restore(request);

        cookieUtil.addCookie(response, "refresh_token", result.refreshToken(),
                "/api/auth", jwtConfig.getRefreshTokenExpiration());

        return ResponseEntity.ok(new RestoreResponse(
                result.loginResponse().accessToken(),
                result.loginResponse().expiresIn(),
                result.refreshToken(),
                "ACTIVE"
        ));
    }
}