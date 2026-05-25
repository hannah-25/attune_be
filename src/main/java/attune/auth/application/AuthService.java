package attune.auth.application;

import attune.auth.application.dto.request.LoginRequest;
import attune.auth.application.dto.request.RestoreRequest;
import attune.auth.application.dto.response.AuthResult;
import attune.auth.application.dto.response.LoginResponse;
import attune.auth.domain.model.UserAuthCache;
import attune.auth.domain.repository.UserAuthCacheRepository;
import attune.common.config.JwtConfig;
import attune.common.error.badrequest.InvalidAccountStatusException;
import attune.common.error.unauthorized.InvalidPasswordException;
import attune.common.error.unauthorized.TokenException;
import attune.common.error.notfound.UserNotFoundException;
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
            throw new TokenException("?≪꽭???좏겙???꾩슂?⑸땲??");
        }

        Claims accessClaims;
        UUID userId;
        UserType userType;
        try {
            accessClaims = jwtProvider.parseExpiredToken(accessToken);
            userId = UUID.fromString(accessClaims.getSubject());
            userType = UserType.valueOf(accessClaims.get("role", String.class));
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            throw new TokenException("?좏슚?섏? ?딆? ?≪꽭???좏겙?낅땲??");
        }

        UserAuthCache cache = userAuthCacheRepository.find(userId)
                .orElseThrow(() -> new TokenException("濡쒓렇???몄뀡??留뚮즺?섏뿀?듬땲?? ?ㅼ떆 濡쒓렇?명빐二쇱꽭??"));

        if (!cache.refreshToken().equals(refreshToken)) {
            throw new TokenException("?좏슚?섏? ?딆? 由ы봽?덉떆 ?좏겙?낅땲??");
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
                .orElseThrow(() -> new UserNotFoundException());

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidPasswordException();
        }

        if (user.getUserStatus() != UserStatus.WITHDRAWAL) {
            throw new InvalidAccountStatusException("?덊눜 ?곹깭??怨꾩젙留?蹂듦뎄?????덉뒿?덈떎.");
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

