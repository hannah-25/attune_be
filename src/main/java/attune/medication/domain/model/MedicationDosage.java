package attune.medication.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity
@Table(
        name = "medication_dosages",
        uniqueConstraints = @UniqueConstraint(columnNames = {"medication_id", "amount"})
)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MedicationDosage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_id", nullable = false)
    private Medication medication;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal amount;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
