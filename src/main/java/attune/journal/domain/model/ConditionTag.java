package attune.journal.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Entity
@Table(name = "condition_tags")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConditionTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 255)
    private String condition;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private ConditionType conditionType;

    @Column(nullable = false)
    private boolean isActive = true;

    public void deactivate() {
        this.isActive = false;
    }
}
