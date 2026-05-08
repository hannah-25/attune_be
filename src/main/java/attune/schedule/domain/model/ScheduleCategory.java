package attune.schedule.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Entity
@Table(name = "schedule_categories")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String categoryName;

    @Column(nullable = false)
    private String color;

    @Column(nullable = false)
    private boolean isActive = true;

    public void update(String categoryName, String color) {
        if (categoryName != null) this.categoryName = categoryName;
        if (color != null) this.color = color;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
