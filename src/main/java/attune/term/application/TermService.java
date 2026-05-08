package attune.term.application;

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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TermService {

    private final TermRepository termRepository;
    private final UserTermAgreementRepository userTermAgreementRepository;

    @Transactional(readOnly = true)
    public List<TermResponse> getLatestTerm() {
        return Arrays.stream(TermType.values())
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
}
