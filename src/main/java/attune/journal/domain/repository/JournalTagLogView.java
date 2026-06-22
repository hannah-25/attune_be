package attune.journal.domain.repository;

import attune.journal.domain.model.JournalTag;
import attune.journal.domain.model.JournalTagLog;

public record JournalTagLogView(JournalTagLog log, JournalTag tag) {}
