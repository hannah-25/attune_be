package attune.term.application.dto.response;

import attune.term.domain.model.Term;

import java.time.LocalDateTime;

public record AdminTermResponse(
        Long id,
        String type,
        Integer version,
        LocalDateTime effectiveAt,
        LocalDateTime createdAt
) {
    public static AdminTermResponse from(Term term) {
        return new AdminTermResponse(
                term.getId(),
                term.getType().name(),
                term.getVersion(),
                term.getEffectiveAt(),
                term.getCreatedAt()
        );
    }
}
