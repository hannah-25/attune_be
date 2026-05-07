package attune.medication.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@Table(name = "medications")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Medication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String genericName;

    @Column(columnDefinition = "TEXT")
    private String effect;

    @Column(columnDefinition = "TEXT")
    private String sideEffect;

    @Column(columnDefinition = "TEXT")
    private String graphUrl;

    private String formulation;

    private String typicalDosageRange;

    private String medicationClass;
}
