package attune.journal.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "condition_logs")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConditionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long conditionTagId;

    @Column(nullable = false)
    private LocalDateTime checkedAt;
}
