package attune.journal.domain.application.dto.response;

import java.time.LocalDate;

public record DeleteJournalResponse(
        LocalDate deletedDate,
        boolean success
) {}
