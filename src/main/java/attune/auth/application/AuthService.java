package attune.auth.application;

import attune.auth.application.dto.request.LoginRequest;
import attune.auth.application.dto.request.RestoreRequest;
import attune.auth.application.dto.response.AuthResult;
import attune.auth.application.dto.response.LoginResponse;
import attune.auth.domain.model.UserAuthCache;
import attune.auth.domain.repository.UserAuthCacheRepository;
import attune.common.config.JwtConfig;
import attune.common.error.InvalidAccountStatusException;
import attune.common.error.InvalidPasswordException;
import attune.common.error.TokenException;
import attune.common.security.CustomUserDetails;
import attune.common.util.JwtProvider;
import attune.user.domain.model.User;
import attune.user.domain.model.UserStatus;
import attune.user.domain.model.UserType;
import attune.user.domain.repository.UserRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final JwtConfig jwtConfig;
    private final UserAuthCacheRepository userAuthCacheRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthResult login(LoginRequest request) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        String accessToken = jwtProvider.generateAccessToken(userDetails.getId(), userDetails.getUserType(), userDetails.getUserStatus());
        String refreshToken = jwtProvider.generateRefreshToken();
        userAuthCacheRepository.save(userDetails.getId(), refreshToken, userDetails.getUserStatus(), jwtConfig.getRefreshTokenExpiration());

        return new AuthResult(
                new LoginResponse(accessToken, jwtConfig.getAccessTokenExpiration()),
                refreshToken
        );
    }

    public AuthResult reissue(String refreshToken, String accessToken) {
        if (accessToken == null) {
            throw new TokenException("액세스 토큰이 필요합니다.");
        }

        Claims accessClaims;
        UUID userId;
        UserType userType;
        try {
            accessClaims = jwtProvider.parseExpiredToken(accessToken);
            userId = UUID.fromString(accessClaims.getSubject());
            userType = UserType.valueOf(accessClaims.get("role", String.class));
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            throw new TokenException("유효하지 않은 액세스 토큰입니다.");
        }

        UserAuthCache cache = userAuthCacheRepository.find(userId)
                .orElseThrow(() -> new TokenException("로그인 세션이 만료되었습니다. 다시 로그인해주세요."));

        if (!cache.refreshToken().equals(refreshToken)) {
            throw new TokenException("유효하지 않은 리프레시 토큰입니다.");
        }

        UserStatus userStatus = UserStatus.valueOf(cache.status());

        String newAccessToken = jwtProvider.generateAccessToken(userId, userType, userStatus);
        String newRefreshToken = jwtProvider.generateRefreshToken();
        userAuthCacheRepository.save(userId, newRefreshToken, userStatus, jwtConfig.getRefreshTokenExpiration());

        return new AuthResult(
                new LoginResponse(newAccessToken, jwtConfig.getAccessTokenExpiration()),
                newRefreshToken
        );
    }

    public void logout(UUID userId) {
        userAuthCacheRepository.delete(userId);
    }

    public AuthResult restore(RestoreRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidPasswordException::new);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidPasswordException();
        }

        if (user.getUserStatus() != UserStatus.WITHDRAWAL) {
            throw new InvalidAccountStatusException("탈퇴 상태의 계정만 복구할 수 있습니다.");
        }

        user.restore();

        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getUserType(), UserStatus.ACTIVE);
        String refreshToken = jwtProvider.generateRefreshToken();
        userAuthCacheRepository.save(user.getId(), refreshToken, UserStatus.ACTIVE, jwtConfig.getRefreshTokenExpiration());

        return new AuthResult(
                new LoginResponse(accessToken, jwtConfig.getAccessTokenExpiration()),
                refreshToken
        );
    }
}