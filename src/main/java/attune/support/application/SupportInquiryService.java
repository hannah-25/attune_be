package attune.support.application;

import attune.support.application.dto.request.CreateSupportInquiryRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SupportInquiryService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String supportMailbox;

    public void createInquiry(CreateSupportInquiryRequest request) {
        sendInquiryEmail(request);
    }

    private void sendInquiryEmail(CreateSupportInquiryRequest request) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setFrom(supportMailbox);
            helper.setTo(supportMailbox);
            helper.setReplyTo(request.email());
            helper.setSubject("[Attune] Support Inquiry - " + request.title());
            helper.setText(buildMessage(request), false);
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send support inquiry email.", e);
        }
    }

    private String buildMessage(CreateSupportInquiryRequest request) {
        return """
                Type: %s
                Title: %s
                Reply Email: %s

                Content:
                %s
                """.formatted(
                request.type().name(),
                request.title(),
                request.email(),
                request.content()
        );
    }
}
