package attune.journal.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "user_journal_tag_preferences")
@IdClass(UserJournalTagPreferenceId.class)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserJournalTagPreference {

    @Id
    private UUID userId;

    @Id
    private Long journalTagId;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private boolean visible;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static UserJournalTagPreference create(UUID userId, Long journalTagId, boolean enabled, boolean visible) {
        LocalDateTime now = LocalDateTime.now();
        return UserJournalTagPreference.builder()
                .userId(userId)
                .journalTagId(journalTagId)
                .enabled(enabled)
                .visible(visible)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public void update(boolean enabled, boolean visible) {
        this.enabled = enabled;
        this.visible = visible;
        this.updatedAt = LocalDateTime.now();
    }
}
