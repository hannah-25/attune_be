package attune.journal.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "legacy_journal_tag_mapping")
@IdClass(LegacyJournalTagMappingId.class)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LegacyJournalTagMapping {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private JournalTagCategory legacyCategory;

    @Id
    private Long legacyTagId;

    private UUID userId;

    @Column(nullable = false)
    private Long journalTagId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static LegacyJournalTagMapping create(
            JournalTagCategory category, Long legacyTagId, UUID userId, Long journalTagId
    ) {
        return LegacyJournalTagMapping.builder()
                .legacyCategory(category)
                .legacyTagId(legacyTagId)
                .userId(userId)
                .journalTagId(journalTagId)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
