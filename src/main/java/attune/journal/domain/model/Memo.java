package attune.journal.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "memos",
        uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "journalDate"})
)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Memo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private LocalDate journalDate;

    @Column(columnDefinition = "TEXT")
    private String memo;

    public void update(String memo) {
        if (memo != null) this.memo = memo;
    }
}
