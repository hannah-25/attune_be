package attune.journal.domain.repository;

import attune.journal.domain.model.UserJournalTagPreference;
import attune.journal.domain.model.UserJournalTagPreferenceId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserJournalTagPreferenceRepository
        extends JpaRepository<UserJournalTagPreference, UserJournalTagPreferenceId> {

    List<UserJournalTagPreference> findAllByUserId(UUID userId);

    List<UserJournalTagPreference> findAllByUserIdAndJournalTagIdIn(UUID userId, List<Long> journalTagIds);
}
