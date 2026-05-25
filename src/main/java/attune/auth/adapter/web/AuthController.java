package attune.auth.adapter.web;

import attune.common.ApiVersion;
import attune.common.ClientType;
import attune.common.HttpHeaders;
import attune.auth.application.AuthService;
import attune.auth.application.dto.request.LoginRequest;
import attune.auth.application.dto.request.RestoreRequest;
import attune.auth.application.dto.response.AuthResult;
import attune.auth.application.dto.response.LoginResponse;
import attune.auth.application.dto.response.RestoreResponse;
import attune.common.config.JwtConfig;
import attune.common.util.CookieUtil;
import attune.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.http.HttpStatus;

import java.util.UUID;


@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping(ApiVersion.V1 + "/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;
    private final JwtConfig jwtConfig;

    @Operation(summary = "로그인", description = "이메일/비밀번호로 로그인합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = HttpHeaders.CLIENT_TYPE, defaultValue = "web") String clientTypeHeader,
            HttpServletResponse response
    ) {
        ClientType clientType = ClientType.from(clientTypeHeader);
        AuthResult result = authService.login(request);
        return ResponseEntity.ok(buildResponse(result, clientType, response));
    }

    @Operation(summary = "토큰 재발급", description = "Refresh Token으로 Access Token을 재발급합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재발급 성공"),
            @ApiResponse(responseCode = "401", description = "리프레시 토큰 만료 또는 유효하지 않음")
    })
    @PostMapping("/reissue")
    public ResponseEntity<LoginResponse> reissue(
            @RequestHeader(value = HttpHeaders.CLIENT_TYPE, defaultValue = "web") String clientTypeHeader,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        ClientType clientType = ClientType.from(clientTypeHeader);

        String refreshToken = clientType.isMobile()
                ? request.getHeader(HttpHeaders.REFRESH_TOKEN)
                : cookieUtil.extractCookie(request, HttpHeaders.REFRESH_TOKEN_COOKIE);

        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String authHeader = request.getHeader("Authorization");
        String accessToken = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7) : null;

        AuthResult result = authService.reissue(refreshToken, accessToken);
        return ResponseEntity.ok(buildResponse(result, clientType, response));
    }

    @Operation(summary = "로그아웃", description = "Refresh Token을 삭제하고 로그아웃합니다.")
    @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = HttpHeaders.CLIENT_TYPE, defaultValue = "web") String clientTypeHeader,
            HttpServletResponse response
    ) {
        ClientType clientType = ClientType.from(clientTypeHeader);
        UUID userId = SecurityUtils.getCurrentUserUuid();
        authService.logout(userId);
        if (!clientType.isMobile()) {
            cookieUtil.removeCookie(response, HttpHeaders.REFRESH_TOKEN_COOKIE, ApiVersion.V1 + "/auth");
        }
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
            @Valid @RequestBody RestoreRequest request,
            @RequestHeader(value = HttpHeaders.CLIENT_TYPE, defaultValue = "web") String clientTypeHeader,
            HttpServletResponse response
    ) {
        ClientType clientType = ClientType.from(clientTypeHeader);
        AuthResult result = authService.restore(request);
        LoginResponse loginResponse = buildResponse(result, clientType, response);
        return ResponseEntity.ok(new RestoreResponse(
                loginResponse.accessToken(),
                loginResponse.expiresIn(),
                loginResponse.refreshToken(),
                "ACTIVE"
        ));
    }

    /**
     * 클라이언트 타입에 따라 refreshToken 전달 방식을 결정합니다.
     * - 웹: HttpOnly 쿠키로 전달, body에는 포함하지 않음
     * - 앱(iOS/Android): body에 포함, 쿠키 미사용
     */
    private LoginResponse buildResponse(AuthResult result, ClientType clientType, HttpServletResponse response) {
        if (clientType.isMobile()) {
            return new LoginResponse(
                    result.loginResponse().accessToken(),
                    result.loginResponse().expiresIn(),
                    result.refreshToken()
            );
        }
        cookieUtil.addCookie(response, HttpHeaders.REFRESH_TOKEN_COOKIE, result.refreshToken(),
                ApiVersion.V1 + "/auth", jwtConfig.getRefreshTokenExpiration());
        return result.loginResponse();
    }
}
