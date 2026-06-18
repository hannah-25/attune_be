package attune.journal.domain.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class UserJournalTagPreferenceId implements Serializable {
    private UUID userId;
    private Long journalTagId;
}
