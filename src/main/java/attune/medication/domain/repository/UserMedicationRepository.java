package attune.medication.domain.repository;

import attune.medication.domain.model.UserMedication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserMedicationRepository extends JpaRepository<UserMedication, Long> {
    Optional<UserMedication> findByIdAndUserId(Long id, UUID userId);

    @Query("""
            SELECT um
            FROM UserMedication um
            JOIN FETCH um.medicationDosage md
            JOIN FETCH md.medication m
            LEFT JOIN FETCH um.consultation c
            WHERE um.user.id = :userId
            ORDER BY um.isActive DESC, um.createdAt DESC, um.id DESC
            """)
    List<UserMedication> findAllByUserIdWithDetails(@Param("userId") UUID userId);
}
