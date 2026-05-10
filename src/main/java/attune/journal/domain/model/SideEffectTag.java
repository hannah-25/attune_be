package attune.journal.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Entity
@Table(name = "side_effect_tags")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SideEffectTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 255)
    private String sideEffect;

    @Column(nullable = false)
    private boolean isActive = true;

    public void deactivate() {
        this.isActive = false;
    }
}
