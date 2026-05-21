package attune.journal.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    @Column(nullable = false)
    private LocalDateTime checkedAt;
}
