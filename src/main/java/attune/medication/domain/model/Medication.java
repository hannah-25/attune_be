package attune.medication.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@Table(
        name = "medications",
        uniqueConstraints = @UniqueConstraint(name = "uk_medications_name", columnNames = "name")
)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Medication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "generic_name", length = 255)
    private String genericName;

    @Column(columnDefinition = "TEXT")
    private String effect;

    @Column(name = "side_effect", columnDefinition = "TEXT")
    private String sideEffect;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "graph_url", columnDefinition = "TEXT")
    private String graphUrl;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(length = 255)
    private String formulation;

    @Column(name = "typical_dosage_range", columnDefinition = "TEXT")
    private String typicalDosageRange;

    @Column(name = "drug_class", length = 255)
    private String drugClass;

    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;
}
