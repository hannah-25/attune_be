package attune.onboarding.domain.model;

import attune.user.domain.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "onboarding_symptoms")
public class OnboardingSymptom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String emotionalEvent;

    private LocalDateTime savedAt;

    // 경로 B: 이미 진단받은 사용자가 직접 체크한 취약 영역
    @Column
    private String selectedSymptomTypes;    // 예: "INATTENTION,TIME_MANAGEMENT"

    @Column
    private String selectedFunctionalAreas; // 예: "업무/학업,시간관리"

    @Column(nullable = false)
    @Builder.Default
    private boolean isQuickOnboarding = false;
}
