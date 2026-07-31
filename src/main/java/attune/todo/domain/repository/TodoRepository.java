package attune.todo.domain.repository;

import attune.todo.domain.model.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    Optional<Todo> findByIdAndUserIdAndIsDeletedFalse(Long id, UUID userId);

    @Query("""
            SELECT t FROM Todo t
            WHERE t.isDeleted = false
              AND t.userId = :userId
              AND t.dueAt >= :startDate
              AND t.dueAt < :endDate
            ORDER BY t.dueAt ASC
            """)
    List<Todo> findAllByDate(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
            SELECT t FROM Todo t
            JOIN User u ON u.id = t.userId
            WHERE t.isDeleted = false
              AND t.isCompleted = false
              AND t.isAllDay = false
              AND t.isAlarmSent = false
              AND t.dueAt >= :from
              AND t.dueAt < :to
              AND u.userStatus = attune.user.domain.model.UserStatus.ACTIVE
            """)
    List<Todo> findAlarmCandidates(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Modifying
    @Transactional
    @Query("UPDATE Todo t SET t.isAlarmSent = true WHERE t.id = :id")
    void markAlarmSentById(@Param("id") Long id);
}
