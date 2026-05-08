package attune.term.application.dto.response;

import attune.term.domain.model.Term;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record TermResponse(
        @Schema(description = "약관 ID") Long id,
        @Schema(description = "약관 버전") Integer version,
        @Schema(description = "약관 타입 (TERMS_OF_SERVICE, PRIVACY_POLICY, MARKETING_CONSENT)") String type,
        @Schema(description = "약관 내용") String content,
        @Schema(description = "약관 시행일") LocalDateTime effectiveAt
) {
    public static TermResponse from(Term term) {
        return new TermResponse(
                term.getId(),
                term.getVersion(),
                term.getType().name(),
                term.getContent(),
                term.getEffectiveAt()
        );
    }
}
