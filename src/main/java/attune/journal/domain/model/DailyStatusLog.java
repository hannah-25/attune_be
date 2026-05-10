package attune.journal.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.openapitools.jackson.nullable.JsonNullable;

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

    public void update(JsonNullable<Float> sleepHour,
                       JsonNullable<SleepQuality> sleepQuality,
                       JsonNullable<Boolean> ateBreakfast,
                       JsonNullable<Boolean> ateLunch,
                       JsonNullable<Boolean> ateDinner) {
        if (sleepHour.isPresent()) this.sleepHour = sleepHour.get();
        if (sleepQuality.isPresent()) this.sleepQuality = sleepQuality.get();
        if (ateBreakfast.isPresent()) this.ateBreakfast = ateBreakfast.get();
        if (ateLunch.isPresent()) this.ateLunch = ateLunch.get();
        if (ateDinner.isPresent()) this.ateDinner = ateDinner.get();
    }
}
