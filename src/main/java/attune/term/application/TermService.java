package attune.term.application;

import attune.common.error.InvalidTermException;
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
        Term latestTerm = termRepository.findTopByOrderByVersionDesc()
                .orElseThrow(TermNotFoundException::new);

        if (!latestTerm.getId().equals(termId)) {
            throw new InvalidTermException();
        }

        LocalDateTime now = LocalDateTime.now();

        userTermAgreementRepository.save(UserTermAgreement.builder()
                .term(latestTerm)
                .user(user)
                .termsOfService(termsOfService)
                .privacyPolicy(privacyPolicy)
                .marketingConsent(marketingConsent)
                .notifiedAt(now)
                .agreedAt(now)
                .build());
    }
}