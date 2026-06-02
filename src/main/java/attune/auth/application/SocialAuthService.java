package attune.auth.application;

import attune.auth.application.dto.OAuthUserInfo;
import attune.auth.application.dto.request.SocialLoginRequest;
import attune.auth.application.dto.response.AuthResult;
import attune.auth.application.dto.response.LoginResponse;
import attune.auth.domain.repository.UserAuthCacheRepository;
import attune.common.config.JwtConfig;
import attune.common.error.BadRequestException;
import attune.common.error.UnauthorizedException;
import attune.common.util.JwtProvider;
import attune.user.application.AccountService;
import attune.user.domain.model.OAuthProvider;
import attune.user.domain.model.User;
import attune.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class SocialAuthService {

    private final List<OAuthVerifier> verifiers;
    private final UserRepository userRepository;
    private final AccountService accountService;
    private final JwtProvider jwtProvider;
    private final JwtConfig jwtConfig;
    private final UserAuthCacheRepository userAuthCacheRepository;

    public AuthResult login(SocialLoginRequest request) {
        OAuthVerifier verifier = verifiers.stream()
                .filter(v -> v.provider() == request.provider())
                .findFirst()
                .orElseThrow(() -> new BadRequestException("지원하지 않는 소셜 로그인입니다."));

        OAuthUserInfo info = verifier.verify(request.token());
        User user = findOrCreateUser(request.provider(), info);

        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getUserType(), user.getUserStatus());
        String refreshToken = jwtProvider.generateRefreshToken();
        userAuthCacheRepository.save(user.getId(), refreshToken, user.getUserStatus(), jwtConfig.getRefreshTokenExpiration());

        return new AuthResult(
                new LoginResponse(accessToken, jwtConfig.getAccessTokenExpiration()),
                refreshToken
        );
    }

    private User findOrCreateUser(OAuthProvider provider, OAuthUserInfo info) {
        Optional<User> byProvider = userRepository.findByProviderAndProviderId(provider, info.providerId());
        if (byProvider.isPresent()) {
            return applyStatusPolicy(byProvider.get());
        }

        if (info.email() != null) {
            Optional<User> byEmail = userRepository.findByEmail(info.email());
            if (byEmail.isPresent()) {
                User user = byEmail.get();
                user.linkSocialProvider(provider, info.providerId());
                return applyStatusPolicy(user);
            }
        }

        if (info.email() == null) {
            throw new BadRequestException("이메일 정보가 필요합니다. 소셜 로그인을 재시도해주세요.");
        }

        return accountService.createSocialUser(info.email(), info.nickname(), provider, info.providerId());
    }

    private User applyStatusPolicy(User user) {
        switch (user.getUserStatus()) {
            case SUSPENDED -> throw new UnauthorizedException("정지된 계정입니다.");
            case WITHDRAWAL -> user.restore();
            case PENDING -> user.activate();
            case ACTIVE -> {}
        }
        return user;
    }
}
