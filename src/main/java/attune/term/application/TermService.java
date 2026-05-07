package attune.term.application;

import attune.common.error.InvalidTermException;
import attune.common.error.notfound.TermNotFoundException;
import attune.term.application.dto.response.TermResponse;
import attune.term.domain.model.Term;
import attune.term.domain.model.TermType;
import attune.term.domain.model.UserTermAgreement;
import attune.term.domain.repository.TermRepository;
import attune.term.domain.repository.UserTermAgreementRepository;
import attune.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TermService {

    private final TermRepository termRepository;
    private final UserTermAgreementRepository userTermAgreementRepository;

    @Transactional(readOnly = true)
    public List<TermResponse> getLatestTerm() {
        Term latest = termRepository.findTopByOrderByVersionDesc()
                .orElseThrow(TermNotFoundException::new);
        return termRepository.findAllByVersion(latest.getVersion()).stream()
                .map(TermResponse::from)
                .toList();
    }

    @Transactional
    public void saveAgreement(User user, Long termId, boolean termsOfService, boolean privacyPolicy, boolean marketingConsent) {
        Term term = termRepository.findById(termId)
                .orElseThrow(TermNotFoundException::new);
        Term latestTerm = termRepository.findTopByOrderByVersionDesc()
                .orElseThrow(TermNotFoundException::new);

        if (!term.getVersion().equals(latestTerm.getVersion())) {
            throw new InvalidTermException();
        }

        List<Term> allTerms = termRepository.findAllByVersion(term.getVersion());
        Map<TermType, Boolean> agreementMap = Map.of(
                TermType.TERMS_OF_SERVICE, termsOfService,
                TermType.PRIVACY_POLICY, privacyPolicy,
                TermType.MARKETING_CONSENT, marketingConsent
        );

        LocalDateTime now = LocalDateTime.now();
        List<UserTermAgreement> agreements = allTerms.stream()
                .map(t -> UserTermAgreement.builder()
                        .user(user)
                        .term(t)
                        .agreed(agreementMap.getOrDefault(t.getType(), false))
                        .notifiedAt(now)
                        .agreedAt(now)
                        .build())
                .toList();

        userTermAgreementRepository.saveAll(agreements);
    }
}
