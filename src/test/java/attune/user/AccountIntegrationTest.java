package attune.user;

import attune.support.IntegrationTest;
import attune.term.domain.model.TermType;
import attune.user.domain.model.EmailVerificationToken;
import attune.user.domain.model.PasswordResetToken;
import attune.user.domain.model.User;
import attune.user.domain.model.UserStatus;
import attune.user.domain.repository.EmailVerificationTokenRepository;
import attune.user.domain.repository.PasswordResetTokenRepository;
import attune.user.domain.repository.UserRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.Properties;

import static attune.support.TestUsers.DEFAULT_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * account/user HTTP->DB 전 구간 통합 테스트.
 */
class AccountIntegrationTest extends IntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @BeforeEach
    void stubMailSender() {
        when(javaMailSender.createMimeMessage())
                .thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
    }

    @Test
    void signupCreatesPendingUserAndEmailVerificationActivatesLogin() throws Exception {
        seedSignupTerms();
        String email = "signup-flow@test.com";

        mockMvc.perform(post("/v1/account/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nickname", "signup-user",
                                "email", email,
                                "password", DEFAULT_PASSWORD,
                                "termsOfService", true,
                                "privacyPolicy", true,
                                "marketingConsent", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(email + " 계정으로 인증 메일을 발송했습니다. 이메일을 확인하여 인증을 완료해주세요."));

        User pendingUser = userRepository.findByEmail(email).orElseThrow();
        assertThat(pendingUser.getUserStatus()).isEqualTo(UserStatus.PENDING);

        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findAll().get(0);
        mockMvc.perform(get("/v1/account/verify-email")
                        .param("token", verificationToken.getToken()))
                .andExpect(status().isOk());

        User activeUser = userRepository.findByEmail(email).orElseThrow();
        assertThat(activeUser.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(emailVerificationTokenRepository.findByToken(verificationToken.getToken())).isEmpty();

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, DEFAULT_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void passwordResetFlowChangesPassword() throws Exception {
        User user = testUsers.activeUser("password-reset@test.com");

        mockMvc.perform(post("/v1/account/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", user.getEmail()))))
                .andExpect(status().isOk());

        PasswordResetToken resetToken = passwordResetTokenRepository.findAll().get(0);
        mockMvc.perform(get("/v1/account/password/reset/{token}", resetToken.getToken()))
                .andExpect(status().isOk());

        String newPassword = "Newpass123!";
        mockMvc.perform(post("/v1/account/password/reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "token", resetToken.getToken(),
                                "newPassword", newPassword))))
                .andExpect(status().isOk());

        assertThat(passwordResetTokenRepository.findByToken(resetToken.getToken())).isEmpty();

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(user.getEmail(), DEFAULT_PASSWORD)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(user.getEmail(), newPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void profileImageNicknameAndSettingsCanBeUpdated() throws Exception {
        User user = testUsers.activeUser("profile-settings@test.com");

        mockMvc.perform(get("/v1/users/settings")
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.medicationNotification").value(true))
                .andExpect(jsonPath("$.theme").value("SYSTEM"))
                .andExpect(jsonPath("$.timezone").value("Asia/Seoul"));

        mockMvc.perform(patch("/v1/users/settings")
                        .header("Authorization", testUsers.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "medicationNotification", false,
                                "reportNotification", false,
                                "marketingNotification", true,
                                "communityNotification", false,
                                "todoNotification", true,
                                "takeMedicationOnHoliday", true,
                                "theme", "DARK",
                                "timezone", "America/New_York"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.medicationNotification").value(false))
                .andExpect(jsonPath("$.marketingNotification").value(true))
                .andExpect(jsonPath("$.theme").value("DARK"))
                .andExpect(jsonPath("$.timezone").value("America/New_York"));

        mockMvc.perform(put("/v1/users/me/nickname")
                        .header("Authorization", testUsers.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("newNickName", "profile-user"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/v1/users/me/image")
                        .header("Authorization", testUsers.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("profileImageUrl", "https://cdn.test/profile.png"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/users/me/profile")
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("profile-user"))
                .andExpect(jsonPath("$.profileImageUrl").value("https://cdn.test/profile.png"))
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.notifications.medication").value(false))
                .andExpect(jsonPath("$.notifications.marketing").value(true));
    }

    @Test
    void withdrawRequiresPasswordThenLoginRequiresRestore() throws Exception {
        User user = testUsers.activeUser("withdraw-account@test.com");

        mockMvc.perform(post("/v1/account/withdraw")
                        .header("Authorization", testUsers.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("password", "wrong-password"))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/v1/account/withdraw")
                        .header("Authorization", testUsers.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("password", DEFAULT_PASSWORD))))
                .andExpect(status().isNoContent());

        User withdrawn = userRepository.findByEmail(user.getEmail()).orElseThrow();
        assertThat(withdrawn.getUserStatus()).isEqualTo(UserStatus.WITHDRAWAL);
        assertThat(withdrawn.getWithdrawalAt()).isNotNull();

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(user.getEmail(), DEFAULT_PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        mockMvc.perform(post("/v1/auth/restore")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(user.getEmail(), DEFAULT_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        User restored = userRepository.findByEmail(user.getEmail()).orElseThrow();
        assertThat(restored.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(restored.getWithdrawalAt()).isNull();
    }

    private void seedSignupTerms() {
        referenceData.term(TermType.TERMS_OF_SERVICE, 1);
        referenceData.term(TermType.PRIVACY_POLICY, 1);
        referenceData.term(TermType.MARKETING_CONSENT, 1);
    }

    private String loginBody(String email, String password) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "email", email,
                "password", password));
    }
}
