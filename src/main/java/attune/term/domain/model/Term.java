package attune.term.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "terms")
public class Term {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TermType type;

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime effectiveAt;

    private LocalDateTime createdAt;
}
