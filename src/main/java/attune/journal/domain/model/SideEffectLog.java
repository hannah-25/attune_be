package attune.journal.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "side_effect_logs")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SideEffectLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sideEffectTagId;

    private UUID userId;

    private Long journalTagId;

    @Column(nullable = false)
    private LocalDateTime checkedAt;
}
