package attune.journal.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "journal_tags",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_journal_tags_owner_category_name_type",
                columnNames = {"owner_key", "category", "name", "tag_type"}
        )
)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JournalTag {

    public static final UUID SYSTEM_OWNER_KEY = new UUID(0L, 0L);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private JournalTagCategory category;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "tag_type", nullable = false, length = 50)
    private String tagType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JournalTagScope scope;

    private UUID ownerUserId;

    @Column(nullable = false)
    private UUID ownerKey;

    @Column(nullable = false)
    private boolean isActive;

    @Column(nullable = false)
    private boolean defaultVisible;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static JournalTag userTag(UUID userId, JournalTagCategory category, String name, String tagType, LocalDateTime now) {
        return JournalTag.builder()
                .category(category)
                .name(name)
                .tagType(tagType)
                .scope(JournalTagScope.USER)
                .ownerUserId(userId)
                .ownerKey(userId)
                .isActive(true)
                .defaultVisible(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public static JournalTag systemTag(
            JournalTagCategory category, String name, String tagType, boolean defaultVisible, LocalDateTime now
    ) {
        return JournalTag.builder()
                .category(category)
                .name(name)
                .tagType(tagType)
                .scope(JournalTagScope.SYSTEM)
                .ownerKey(SYSTEM_OWNER_KEY)
                .isActive(true)
                .defaultVisible(defaultVisible)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public void deactivate(LocalDateTime now) {
        this.isActive = false;
        this.updatedAt = now;
    }

    public void activate(LocalDateTime now) {
        this.isActive = true;
        this.updatedAt = now;
    }

}
