package attune.journal.application.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CheckJournalTagRequest(@NotNull LocalDate journalDate) {}
