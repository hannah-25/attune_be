package attune.common.mail;

import attune.common.error.internalserver.MailSendFailedException;
import attune.common.observability.ObservabilityMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MailServiceTest {

    private JavaMailSender mailSender;
    private MimeMessage mimeMessage;
    private SimpleMeterRegistry meterRegistry;
    private MailService mailService;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        meterRegistry = new SimpleMeterRegistry();
        mailService = new MailService(mailSender, new ObservabilityMetrics(meterRegistry));
        ReflectionTestUtils.setField(mailService, "fromEmail", "no-reply@attune.app");
    }

    @Test
    void sendEmail_doesNotExposeRecipientAddressInExceptionCause() throws MessagingException {
        // SMTP 오류 메시지에는 수신자 주소가 포함될 수 있다 (예: "Invalid Addresses; ... person@example.com")
        doThrow(new MessagingException("Invalid Addresses; person@example.com"))
                .when(mimeMessage).setSubject(anyString(), anyString());

        assertThatThrownBy(() -> mailService.sendWelcomeEmail("person@example.com", "닉네임"))
                .isInstanceOfSatisfying(MailSendFailedException.class, e -> {
                    assertThat(e.getCause()).hasMessage("Mail send failed");
                    assertThat(e.getCause()).hasMessageNotContaining("person@example.com");
                    assertThat(e.getCause()).hasNoCause();
                });

        assertThat(meterRegistry.counter("attune.mail.requests",
                "type", "general", "outcome", "failure").count()).isEqualTo(1.0);
    }
}
