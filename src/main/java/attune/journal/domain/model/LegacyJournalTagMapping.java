package attune.journal.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "legacy_journal_tag_mapping")
@IdClass(LegacyJournalTagMappingId.class)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LegacyJournalTagMapping implements Persistable<LegacyJournalTagMappingId> {

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

    @Transient
    @Builder.Default
    private boolean newEntity = true;

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

    @Override
    public LegacyJournalTagMappingId getId() {
        return new LegacyJournalTagMappingId(legacyCategory, legacyTagId);
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    private void markNotNew() {
        this.newEntity = false;
    }
}
