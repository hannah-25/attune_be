package attune.auth.application;

import attune.auth.application.dto.OAuthUserInfo;
import attune.auth.application.dto.request.SocialLoginRequest;
import attune.auth.application.dto.response.AuthResult;
import attune.auth.application.dto.response.LoginResponse;
import attune.auth.domain.repository.UserAuthCacheRepository;
import attune.common.config.JwtConfig;
import attune.common.error.BadRequestException;
import attune.common.error.ConflictException;
import attune.common.error.UnauthorizedException;
import attune.common.error.badrequest.InvalidAccountStatusException;
import attune.common.error.notfound.UserNotFoundException;
import attune.common.event.UserActivatedEvent;
import attune.user.domain.model.UserStatus;
import attune.common.util.JwtProvider;
import attune.user.application.AccountService;
import attune.user.domain.model.OAuthProvider;
import attune.user.domain.model.User;
import attune.user.domain.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class SocialAuthService {

    private static final String SOCIAL_PROVIDER_CONFLICT_MESSAGE =
            "\uC774\uBBF8 \uB2E4\uB978 \uC18C\uC15C \uACC4\uC815\uC73C\uB85C \uAC00\uC785\uB41C \uC774\uBA54\uC77C\uC785\uB2C8\uB2E4.";

    private final List<OAuthVerifier> verifiers;
    private final UserRepository userRepository;
    private final AccountService accountService;
    private final JwtProvider jwtProvider;
    private final JwtConfig jwtConfig;
    private final UserAuthCacheRepository userAuthCacheRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public AuthResult login(SocialLoginRequest request) {
        OAuthVerifier verifier = verifiers.stream()
                .filter(v -> v.provider() == request.provider())
                .findFirst()
                .orElseThrow(() -> new BadRequestException("지원하지 않는 소셜 로그인입니다."));

        OAuthUserInfo info = verifier.verify(request.token());
        User user = findOrCreateUser(request.provider(), info);
        user.recordLogin(LocalDateTime.now(clock));

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
                if (user.getProvider() != null) {
                    throw new ConflictException(SOCIAL_PROVIDER_CONFLICT_MESSAGE);
                }
                if (user.getUserStatus() == UserStatus.PENDING) {
                    throw new ConflictException("이메일 인증이 완료되지 않은 계정이 존재합니다.");
                }
                user.linkSocialProvider(provider, info.providerId());
                return applyStatusPolicy(user);
            }
        }

        if (info.email() == null) {
            throw new BadRequestException("이메일 정보가 필요합니다. 소셜 로그인을 재시도해주세요.");
        }

        return accountService.createSocialUser(info.email(), info.nickname(), provider, info.providerId());
    }

    public AuthResult restore(SocialLoginRequest request) {
        OAuthVerifier verifier = verifiers.stream()
                .filter(v -> v.provider() == request.provider())
                .findFirst()
                .orElseThrow(() -> new BadRequestException("지원하지 않는 소셜 로그인입니다."));

        OAuthUserInfo info = verifier.verify(request.token());

        User user = userRepository.findByProviderAndProviderId(request.provider(), info.providerId())
                .orElseThrow(UserNotFoundException::new);

        if (user.getUserStatus() != UserStatus.WITHDRAWAL) {
            throw new InvalidAccountStatusException("탈퇴 상태의 계정만 복구할 수 있습니다.");
        }

        user.restore();
        user.recordLogin(LocalDateTime.now(clock));
        eventPublisher.publishEvent(new UserActivatedEvent(user.getId()));

        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getUserType(), UserStatus.ACTIVE);
        String refreshToken = jwtProvider.generateRefreshToken();
        userAuthCacheRepository.save(user.getId(), refreshToken, UserStatus.ACTIVE, jwtConfig.getRefreshTokenExpiration());

        return new AuthResult(
                new LoginResponse(accessToken, jwtConfig.getAccessTokenExpiration()),
                refreshToken
        );
    }

    private User applyStatusPolicy(User user) {
        switch (user.getUserStatus()) {
            case SUSPENDED -> throw new UnauthorizedException("정지된 계정입니다.");
            case WITHDRAWAL -> throw new ConflictException("탈퇴한 계정입니다. 복구 확인이 필요합니다.");
            case PENDING -> {
                user.activate();
                eventPublisher.publishEvent(new UserActivatedEvent(user.getId()));
            }
            case ACTIVE -> {}
        }
        return user;
    }
}
