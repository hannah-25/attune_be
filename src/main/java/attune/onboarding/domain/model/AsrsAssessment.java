package attune.onboarding.domain.model;

import attune.user.domain.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "asrs_assessments",
        indexes = @Index(name = "idx_asrs_assessments_user_completed", columnList = "user_id, completed_at")
)
public class AsrsAssessment {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private int partAScore;
    private int totalScore;
    private LocalDateTime completedAt;

    @ElementCollection
    @CollectionTable(name = "asrs_answers", joinColumns = @JoinColumn(name = "assessment_id"))
    @Builder.Default
    private List<AsrsAnswer> answers = new ArrayList<>();
}
