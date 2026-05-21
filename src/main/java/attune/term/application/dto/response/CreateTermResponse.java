package attune.term.application.dto.response;

import attune.term.domain.model.Term;
import attune.term.domain.model.TermType;

import java.time.LocalDateTime;

public record CreateTermResponse(
        Long id,
        Integer version,
        TermType type,
        LocalDateTime effectiveAt,
        LocalDateTime createdAt
) {
    public static CreateTermResponse from(Term term) {
        return new CreateTermResponse(
                term.getId(),
                term.getVersion(),
                term.getType(),
                term.getEffectiveAt(),
                term.getCreatedAt()
        );
    }
}
