package attune.journal.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Entity
@Table(
        name = "daily_goal_logs",
        uniqueConstraints = @UniqueConstraint(columnNames = {"dailyGoalId", "date"})
)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DailyGoalLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long dailyGoalId;

    @Column(nullable = false)
    private Integer score;

    @Column(nullable = false)
    private LocalDate date;

    public void updateScore(Integer score) {
        this.score = score;
    }
}
