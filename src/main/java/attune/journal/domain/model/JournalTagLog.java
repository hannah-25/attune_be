package attune.journal.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "journal_tag_logs",
        indexes = {
                @Index(name = "idx_journal_tag_logs_user_date_checked", columnList = "user_id, journal_date, checked_at")
        }
)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JournalTagLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private Long journalTagId;

    @Column(nullable = false)
    private LocalDate journalDate;

    @Column(nullable = false)
    private LocalDateTime checkedAt;
}
