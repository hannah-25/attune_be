package attune.support;

import attune.ai.application.AiTextGenerator;
import attune.auth.adapter.oauth.AppleOAuthVerifier;
import attune.auth.adapter.oauth.GoogleOAuthVerifier;
import attune.auth.adapter.oauth.KakaoOAuthVerifier;
import attune.calendar.application.GoogleCalendarClient;
import attune.medicationAnalysis.infrastructure.GeminiReportClient;
import attune.user.domain.model.OAuthProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Properties;

import static org.mockito.Mockito.when;

/**
 * HTTP→DB 전 구간 통합 테스트 베이스.
 *
 * - 실제 HTTP(MockMvc) → JwtAuthenticationFilter(실 JWT) → 컨트롤러 → 서비스 → JPA →
 *   Testcontainers MySQL/Redis 전 구간을 지난다.
 * - 외부 연동 mock은 **반드시 이 클래스에만** 선언한다. 서브클래스가 @MockitoBean을
 *   추가하면 컨텍스트 캐시가 깨져 테스트마다 컨텍스트를 재기동하게 된다.
 * - 푸시는 mock이 필요 없다: notification.push.provider 미설정 시 StubPushSender가
 *   활성(matchIfMissing=true)이고 로그만 남긴다.
 *
 * 사용법: extends IntegrationTest 후 mockMvc/testUsers/referenceData 사용.
 * 계획: docs/exec-plans/active/2026-07-05-http-db-integration-tests.md
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({DatabaseCleaner.class, RedisCleaner.class, TestUsers.class, ReferenceDataFixture.class})
public abstract class IntegrationTest {

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", TestRedisContainer::host);
        registry.add("spring.data.redis.port", TestRedisContainer::port);
    }

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected TestUsers testUsers;
    @Autowired
    protected ReferenceDataFixture referenceData;

    @Autowired
    private DatabaseCleaner databaseCleaner;
    @Autowired
    private RedisCleaner redisCleaner;

    // ---- 외부 연동 mock (여기에만 선언) ----
    @MockitoBean
    protected AiTextGenerator aiTextGenerator;
    @MockitoBean
    protected GeminiReportClient geminiReportClient;
    @MockitoBean
    protected GoogleCalendarClient googleCalendarClient;
    @MockitoBean
    protected JavaMailSender javaMailSender;
    @MockitoBean
    protected GoogleOAuthVerifier googleOAuthVerifier;
    @MockitoBean
    protected AppleOAuthVerifier appleOAuthVerifier;
    @MockitoBean
    protected KakaoOAuthVerifier kakaoOAuthVerifier;

    @BeforeEach
    void resetStateAndStubRouting() {
        databaseCleaner.clean();
        redisCleaner.clean();
        // SocialAuthService는 verifier.provider()로 구현체를 고른다.
        // @MockitoBean은 테스트마다 리셋되므로 기본 stubbing은 @BeforeEach에서 매번 건다.
        when(googleOAuthVerifier.provider()).thenReturn(OAuthProvider.GOOGLE);
        when(appleOAuthVerifier.provider()).thenReturn(OAuthProvider.APPLE);
        when(kakaoOAuthVerifier.provider()).thenReturn(OAuthProvider.KAKAO);
        when(javaMailSender.createMimeMessage())
                .thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
    }
}
