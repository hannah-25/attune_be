package attune.journal.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "trouble_logs")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TroubleLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long troubleTagId;

    @Column(nullable = false)
    private LocalDateTime checkedAt;
}
