package attune.support;

import attune.common.config.JwtConfig;
import attune.common.util.JwtProvider;
import attune.user.domain.model.OAuthProvider;
import attune.user.domain.model.User;
import attune.user.domain.model.UserStatus;
import attune.user.domain.model.UserType;
import attune.user.domain.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 통합 테스트용 사용자 픽스처 + 실제 JWT 발급 헬퍼.
 * 목 인증(.with(user())) 대신 진짜 토큰을 Authorization 헤더에 태워
 * JwtAuthenticationFilter 경로까지 검증한다.
 */
public class TestUsers {

    public static final String DEFAULT_PASSWORD = "Abcd1234!";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final JwtConfig jwtConfig;

    public TestUsers(UserRepository userRepository, PasswordEncoder passwordEncoder,
                     JwtProvider jwtProvider, JwtConfig jwtConfig) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.jwtConfig = jwtConfig;
    }

    public User activeUser(String email) {
        return activeUser(email, DEFAULT_PASSWORD);
    }

    public User activeUser(String email, String rawPassword) {
        return userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .nickname("tester")
                .userType(UserType.USER)
                .userStatus(UserStatus.ACTIVE)
                .build());
    }

    public User activeAdmin(String email) {
        return userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                .nickname("admin-tester")
                .userType(UserType.ADMIN)
                .userStatus(UserStatus.ACTIVE)
                .build());
    }

    public User suspendedUser(String email) {
        return userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                .nickname("suspended-tester")
                .userType(UserType.USER)
                .userStatus(UserStatus.SUSPENDED)
                .build());
    }

    public User withdrawnSocialUser(String email, OAuthProvider provider, String providerId) {
        return userRepository.save(User.builder()
                .email(email)
                .nickname("withdrawn-social")
                .userType(UserType.USER)
                .userStatus(UserStatus.WITHDRAWAL)
                .withdrawalAt(LocalDateTime.now().minusDays(1))
                .provider(provider)
                .providerId(providerId)
                .build());
    }

    public String accessToken(User user) {
        return jwtProvider.generateAccessToken(user.getId(), user.getUserType(), user.getUserStatus());
    }

    /** 이미 만료된 access token — reissue의 parseExpiredToken 경로 검증용. */
    public String expiredAccessToken(User user) {
        Date expiredAt = new Date(System.currentTimeMillis() - 10_000);
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("role", user.getUserType().toString())
                .claim("status", user.getUserStatus().toString())
                .issuedAt(new Date(expiredAt.getTime() - 60_000))
                .expiration(expiredAt)
                .signWith(Keys.hmacShaKeyFor(jwtConfig.getJwtSecret().getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    /** MockMvc Authorization 헤더 값. */
    public String bearer(User user) {
        return "Bearer " + accessToken(user);
    }
}
