package attune.support;

import attune.common.util.JwtProvider;
import attune.user.domain.model.User;
import attune.user.domain.model.UserStatus;
import attune.user.domain.model.UserType;
import attune.user.domain.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

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

    public TestUsers(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtProvider jwtProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
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

    public String accessToken(User user) {
        return jwtProvider.generateAccessToken(user.getId(), user.getUserType(), user.getUserStatus());
    }

    /** MockMvc Authorization 헤더 값. */
    public String bearer(User user) {
        return "Bearer " + accessToken(user);
    }
}
