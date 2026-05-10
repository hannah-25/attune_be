package attune.journal.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Entity
@Table(
        name = "daily_goals",
        uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "dailyGoal"})
)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DailyGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 500)
    private String dailyGoal;

    @Column(nullable = false)
    private boolean isActive = true;

    public void deactivate() {
        this.isActive = false;
    }

    public void reactivate() {
        this.isActive = true;
    }
}
