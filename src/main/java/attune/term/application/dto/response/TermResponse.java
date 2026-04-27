package attune.term.application.dto.response;

import attune.term.domain.model.Term;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record TermResponse(
        @Schema(description = "약관 ID") Long id,
        @Schema(description = "약관 버전") Integer version,
        @Schema(description = "이용약관 내용") String termsOfService,
        @Schema(description = "개인정보 처리방침 내용") String privacyPolicy,
        @Schema(description = "마케팅 수신 동의 내용") String marketingConsent,
        @Schema(description = "약관 개정일") LocalDateTime createdAt,
        @Schema(description = "약관 시행일") LocalDateTime effectiveAt
) {
    public static TermResponse from(Term term) {
        return new TermResponse(
                term.getId(),
                term.getVersion(),
                term.getTermsOfService(),
                term.getPrivacyPolicy(),
                term.getMarketingConsent(),
                term.getCreatedAt(),
                term.getEffectiveAt()
        );
    }
}