package attune.support.application;

import attune.common.mail.MailService;
import attune.common.util.SecurityUtils;
import attune.support.application.dto.request.CreateSupportInquiryRequest;
import attune.support.domain.model.SupportInquiry;
import attune.support.domain.repository.SupportInquiryRepository;
import attune.user.domain.model.User;
import attune.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

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

        mailService.sendInquiryEmail(
                inquiry.getEmail(),
                inquiry.getType().name(),
                inquiry.getTitle(),
                inquiry.getContent()
        );
    }
}
