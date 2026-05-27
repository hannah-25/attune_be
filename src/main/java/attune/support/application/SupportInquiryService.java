package attune.support.application;

import attune.common.util.SecurityUtils;
import attune.support.application.dto.request.CreateSupportInquiryRequest;
import attune.support.domain.model.SupportInquiry;
import attune.support.domain.repository.SupportInquiryRepository;
import attune.user.domain.model.User;
import attune.user.domain.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupportInquiryService {

    private final SupportInquiryRepository supportInquiryRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String supportMailbox;

    @Transactional
    public void createInquiry(CreateSupportInquiryRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        SupportInquiry inquiry = SupportInquiry.builder()
                .user(user)
                .type(request.type())
                .title(request.title())
                .content(request.content())
                .email(request.email())
                .createdAt(LocalDateTime.now())
                .build();

        supportInquiryRepository.save(inquiry);

        sendInquiryEmailAsync(inquiry);
    }

    @Async
    public void sendInquiryEmailAsync(SupportInquiry inquiry) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setFrom(supportMailbox);
            helper.setTo(supportMailbox);
            helper.setReplyTo(inquiry.getEmail());
            helper.setSubject("[Attune 문의] " + inquiry.getTitle());
            helper.setText(buildMessage(inquiry), false);
            mailSender.send(mimeMessage);
            log.info("문의 이메일 발송 완료 — inquiryId={}", inquiry.getId());
        } catch (MessagingException e) {
            log.error("문의 이메일 발송 실패 — inquiryId={}, error={}", inquiry.getId(), e.getMessage(), e);
        }
    }

    private String buildMessage(SupportInquiry inquiry) {
        return """
                [문의 유형] %s
                [제목] %s
                [연락처] %s

                [내용]
                %s
                """.formatted(
                inquiry.getType().name(),
                inquiry.getTitle(),
                inquiry.getEmail(),
                inquiry.getContent()
        );
    }
}
