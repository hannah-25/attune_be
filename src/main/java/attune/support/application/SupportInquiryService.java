package attune.support.application;

import attune.common.mail.MailService;
import attune.common.util.SecurityUtils;
import attune.support.application.dto.request.CreateSupportInquiryRequest;
import attune.support.domain.model.SupportInquiry;
import attune.support.domain.repository.SupportInquiryRepository;
import attune.user.domain.model.User;
import attune.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final MailService mailService;

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
            mailService.sendInquiryEmail(
                    inquiry.getEmail(),
                    inquiry.getType().name(),
                    inquiry.getTitle(),
                    inquiry.getContent()
            );
            log.info("문의 이메일 발송 완료 — inquiryId={}", inquiry.getId());
        } catch (Exception e) {
            log.error("문의 이메일 발송 실패 — inquiryId={}, error={}", inquiry.getId(), e.getMessage(), e);
        }
    }
}
