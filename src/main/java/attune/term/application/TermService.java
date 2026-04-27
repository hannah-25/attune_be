package attune.term.application;

import attune.common.error.notfound.TermNotFoundException;
import attune.term.application.dto.response.TermResponse;
import attune.term.domain.model.Term;
import attune.term.domain.model.UserTermAgreement;
import attune.term.domain.repository.TermRepository;
import attune.term.domain.repository.UserTermAgreementRepository;
import attune.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TermService {

    private final TermRepository termRepository;
    private final UserTermAgreementRepository userTermAgreementRepository;

    @Transactional(readOnly = true)
    public TermResponse getLatestTerm() {
        Term term = termRepository.findTopByOrderByVersionDesc()
                .orElseThrow(TermNotFoundException::new);
        return TermResponse.from(term);
    }

    @Transactional
    public void saveAgreement(User user, Long termId, boolean termsOfService, boolean privacyPolicy, boolean marketingConsent) {
        Term term = termRepository.findById(termId)
                .orElseThrow(TermNotFoundException::new);

        userTermAgreementRepository.save(UserTermAgreement.builder()
                .user(user)
                .term(term)
                .termsOfService(termsOfService)
                .privacyPolicy(privacyPolicy)
                .marketingConsent(marketingConsent)
                .agreedAt(LocalDateTime.now())
                .build());
    }
}