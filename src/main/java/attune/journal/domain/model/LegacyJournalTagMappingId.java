package attune.journal.domain.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class LegacyJournalTagMappingId implements Serializable {
    private JournalTagCategory legacyCategory;
    private Long legacyTagId;
}
