package attune.journal.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "daily_status_logs",
        uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "date"})
)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DailyStatusLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID userId;

    private Float sleepHour;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private SleepQuality sleepQuality;

    private Boolean ateBreakfast;

    private Boolean ateLunch;

    private Boolean ateDinner;

    @Column(nullable = false)
    private LocalDate date;

    public void update(Float sleepHour, SleepQuality sleepQuality,
                       Boolean ateBreakfast, Boolean ateLunch, Boolean ateDinner) {
        this.sleepHour = sleepHour;
        this.sleepQuality = sleepQuality;
        this.ateBreakfast = ateBreakfast;
        this.ateLunch = ateLunch;
        this.ateDinner = ateDinner;
    }
}
