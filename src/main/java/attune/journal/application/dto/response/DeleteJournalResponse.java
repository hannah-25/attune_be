package attune.journal.application.dto.response;

import java.time.LocalDate;

public record DeleteJournalResponse(
        LocalDate deletedDate,
        boolean success
) {}
