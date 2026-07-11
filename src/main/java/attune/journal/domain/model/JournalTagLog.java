package attune.journal.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
        // 운영 DDL(20260623_replace_legacy_journal_tags.sql)에만 있고 엔티티에는 없던 제약.
        // 테스트 DB는 ddl-auto=create-drop으로 엔티티에서 생성되므로, 선언하지 않으면
        // JournalTagLogSaver의 유니크 위반 catch 경로가 통합 테스트에서 실행되지 않는다.
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_journal_tag_logs_daily_check", columnNames = {"user_id", "journal_tag_id", "journal_date"})
        },
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
