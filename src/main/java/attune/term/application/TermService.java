package attune.term.application;

import attune.common.error.notfound.TermNotFoundException;
import attune.common.mail.event.TermsUpdatedEvent;
import attune.common.util.SecurityUtils;
import attune.term.application.dto.request.CreateTermRequest;
import attune.term.application.dto.response.AdminTermResponse;
import attune.term.application.dto.response.CreateTermResponse;
import attune.term.application.dto.response.TermResponse;
import attune.term.domain.model.Term;
import attune.term.domain.model.TermType;
import attune.term.domain.model.UserTermAgreement;
import attune.term.domain.repository.TermRepository;
import attune.term.domain.repository.UserTermAgreementRepository;
import attune.user.domain.model.User;
import attune.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TermService {

    private final TermRepository termRepository;
    private final UserTermAgreementRepository userTermAgreementRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CreateTermResponse createTerm(CreateTermRequest request) {
        Term term = Term.builder()
                .version(request.version())
                .type(request.type())
                .content(request.content())
                .effectiveAt(request.effectiveDate())
                .createdAt(request.createdAt())
                .build();
        Term saved = termRepository.save(term);

        if (request.sendEmail()) {
            eventPublisher.publishEvent(new TermsUpdatedEvent(request.title(), request.content()));
        }

        return CreateTermResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<AdminTermResponse> getAllTermsForAdmin() {
        return termRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(AdminTermResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TermResponse> getLatestTerm() {
        return Arrays.stream(TermType.values())
                .filter(type -> type != TermType.AI_ANALYSIS_CONSENT)
                .map(type -> termRepository.findTopByTypeOrderByVersionDesc(type)
                        .orElseThrow(TermNotFoundException::new))
                .map(TermResponse::from)
                .toList();
    }

    @Transactional
    public void saveAgreement(User user, boolean termsOfService, boolean privacyPolicy, boolean marketingConsent) {
        Map<TermType, Boolean> agreementMap = Map.of(
                TermType.TERMS_OF_SERVICE, termsOfService,
                TermType.PRIVACY_POLICY, privacyPolicy,
                TermType.MARKETING_CONSENT, marketingConsent
        );

        LocalDateTime now = LocalDateTime.now();
        List<UserTermAgreement> agreements = Arrays.stream(TermType.values())
                .filter(type -> type != TermType.AI_ANALYSIS_CONSENT)
                .map(type -> {
                    Term term = termRepository.findTopByTypeOrderByVersionDesc(type)
                            .orElseThrow(TermNotFoundException::new);
                    return UserTermAgreement.builder()
                            .user(user)
                            .term(term)
                            .agreed(agreementMap.getOrDefault(type, false))
                            .notifiedAt(now)
                            .agreedAt(now)
                            .build();
                })
                .toList();

        userTermAgreementRepository.saveAll(agreements);
    }

    @Transactional
    public void consentAiAnalysis() {
        saveAiAnalysisAgreement(true);
    }

    @Transactional
    public void revokeAiAnalysis() {
        saveAiAnalysisAgreement(false);
    }

    @Transactional(readOnly = true)
    public boolean isAiAnalysisConsented(UUID userId) {
        return userTermAgreementRepository
                .findTopByUser_IdAndTerm_TypeOrderByAgreedAtDesc(userId, TermType.AI_ANALYSIS_CONSENT)
                .map(UserTermAgreement::isAgreed)
                .orElse(false);
    }

    private void saveAiAnalysisAgreement(boolean agreed) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        User user = userRepository.getReferenceById(userId);
        Term term = termRepository.findTopByTypeOrderByVersionDesc(TermType.AI_ANALYSIS_CONSENT)
                .orElseThrow(TermNotFoundException::new);
        LocalDateTime now = LocalDateTime.now();
        userTermAgreementRepository.save(UserTermAgreement.builder()
                .user(user)
                .term(term)
                .agreed(agreed)
                .agreedAt(now)
                .notifiedAt(now)
                .build());
    }
}
