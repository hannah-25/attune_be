package attune.auth.application;

import attune.auth.application.dto.request.LoginRequest;
import attune.auth.application.dto.response.LoginResponse;
import attune.auth.domain.model.RefreshToken;
import attune.auth.domain.repository.RefreshTokenRepository;
import attune.common.config.JwtConfig;
import attune.common.error.TokenException;
import attune.common.error.notfound.UserNotFoundException;
import attune.common.security.CustomUserDetails;
import attune.common.util.JwtTokenGenerator;
import attune.common.util.JwtTokenValidator;
import attune.user.domain.model.User;
import attune.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenGenerator jwtTokenGenerator;
    private final JwtTokenValidator jwtTokenValidator;
    private final JwtConfig jwtConfig;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public LoginResponse login(LoginRequest request) {
        // AuthenticationManager가 CustomUserDetailsService를 통해 인증 수행
        // 비밀번호 불일치, 계정 비활성 등은 여기서 예외 발생
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(UserNotFoundException::new);

        // Access Token 생성
        String accessToken = jwtTokenGenerator.generateAccessToken(user);

        // Refresh Token 생성 및 DB 저장
        String refreshToken = jwtTokenGenerator.generateRefreshToken(user);
        saveRefreshToken(user.getId(), refreshToken);

        return new LoginResponse(accessToken, jwtConfig.getAccessTokenExpiration());
    }

    public LoginResponse reissue(String refreshToken) {
        // 토큰 유효성 검증
        if (!jwtTokenValidator.validateToken(refreshToken) || jwtTokenValidator.isTokenExpired(refreshToken)) {
            throw new TokenException("리프레시 토큰이 만료되었습니다. 다시 로그인해주세요.");
        }

        // DB에 저장된 RT와 일치하는지 확인
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new TokenException("유효하지 않은 리프레시 토큰입니다."));

        if (storedToken.isExpired()) {
            refreshTokenRepository.delete(storedToken);
            throw new TokenException("리프레시 토큰이 만료되었습니다. 다시 로그인해주세요.");
        }

        UUID userId = jwtTokenValidator.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        // 새 토큰 발급
        String newAccessToken = jwtTokenGenerator.generateAccessToken(user);
        String newRefreshToken = jwtTokenGenerator.generateRefreshToken(user);

        // DB의 RT 갱신
        storedToken.updateToken(newRefreshToken, calculateRefreshTokenExpiry());

        return new LoginResponse(newAccessToken, jwtConfig.getAccessTokenExpiration());
    }

    public String getNewRefreshToken(String oldRefreshToken) {
        // reissue 시 새로 생성된 RT를 Controller에서 Cookie로 설정하기 위해 조회
        UUID userId = jwtTokenValidator.getUserIdFromToken(oldRefreshToken);
        RefreshToken storedToken = refreshTokenRepository.findByUserId(userId)
                .orElseThrow(() -> new TokenException("리프레시 토큰을 찾을 수 없습니다."));
        return storedToken.getToken();
    }

    public UUID getUserIdFromAccessToken(String accessToken) {
        return jwtTokenValidator.getUserIdFromToken(accessToken);
    }

    public String getRefreshTokenByUserId(UUID userId) {
        RefreshToken storedToken = refreshTokenRepository.findByUserId(userId)
                .orElseThrow(() -> new TokenException("리프레시 토큰을 찾을 수 없습니다."));
        return storedToken.getToken();
    }

    public void logout(UUID userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    private void saveRefreshToken(UUID userId, String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByUserId(userId)
                .orElse(null);

        if (refreshToken != null) {
            refreshToken.updateToken(token, calculateRefreshTokenExpiry());
        } else {
            refreshToken = RefreshToken.builder()
                    .userId(userId)
                    .token(token)
                    .expiresAt(calculateRefreshTokenExpiry())
                    .build();
            refreshTokenRepository.save(refreshToken);
        }
    }

    private LocalDateTime calculateRefreshTokenExpiry() {
        return LocalDateTime.now().plusSeconds(jwtConfig.getRefreshTokenExpiration() / 1000);
    }
}