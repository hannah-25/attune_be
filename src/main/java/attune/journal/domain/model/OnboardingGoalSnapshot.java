package attune.journal.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "onboarding_goal_snapshots")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingGoalSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_goal_id", nullable = false)
    private DailyGoal dailyGoal;

    @Column(nullable = false)
    private LocalDateTime onboardingTime;
}
