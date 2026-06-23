package attune.journal.application.dto.response;

import attune.journal.domain.model.JournalTagCategory;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record JournalTagCheckResponse(
        Long tagId,
        JournalTagCategory category,
        String name,
        String tagType,
        LocalDate journalDate,
        LocalDateTime checkedAt
) {}
