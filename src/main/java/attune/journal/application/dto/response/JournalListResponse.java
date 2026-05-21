package attune.journal.application.dto.response;

import java.time.LocalDate;
import java.util.List;

public record JournalListResponse(
        List<LocalDate> dates
) {}
