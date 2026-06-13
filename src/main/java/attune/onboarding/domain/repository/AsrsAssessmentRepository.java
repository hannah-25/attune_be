package attune.onboarding.domain.repository;

import attune.onboarding.domain.model.AsrsAssessment;
import attune.user.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AsrsAssessmentRepository extends JpaRepository<AsrsAssessment, Long> {
    boolean existsByUser(User user);

    Optional<AsrsAssessment> findTopByUserOrderByCompletedAtDesc(User user);

    @Query("SELECT DISTINCT a FROM AsrsAssessment a LEFT JOIN FETCH a.answers WHERE a.user.id = :userId ORDER BY a.completedAt DESC")
    List<AsrsAssessment> findAllByUserIdWithAnswers(@Param("userId") java.util.UUID userId);

    @Query("SELECT a FROM AsrsAssessment a LEFT JOIN FETCH a.answers WHERE a.id = :id AND a.user.id = :userId")
    Optional<AsrsAssessment> findByIdAndUserIdWithAnswers(@Param("id") Long id, @Param("userId") java.util.UUID userId);
}
