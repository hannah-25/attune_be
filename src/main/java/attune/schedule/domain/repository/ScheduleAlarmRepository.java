package attune.schedule.domain.repository;

import attune.schedule.domain.model.ScheduleAlarm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleAlarmRepository extends JpaRepository<ScheduleAlarm, Long> {

    List<ScheduleAlarm> findAllByScheduleId(Long scheduleId);

    @Modifying
    @Query("DELETE FROM ScheduleAlarm sa WHERE sa.schedule.id = :scheduleId")
    void deleteAllByScheduleId(@Param("scheduleId") Long scheduleId);

    @Transactional(readOnly = true)
    @Query("""
            SELECT sa FROM ScheduleAlarm sa
            JOIN FETCH sa.schedule s
            JOIN User u ON u.id = s.userId
            WHERE sa.id > :afterId
              AND sa.alarmAt >= :from
              AND sa.alarmAt <= :now
              AND sa.isSent = false
              AND s.isDeleted = false
              AND u.userStatus = attune.user.domain.model.UserStatus.ACTIVE
            ORDER BY sa.id ASC
            """)
    List<ScheduleAlarm> findUnsentWithScheduleByAlarmAtBeforeOrEqualAfterId(
            @Param("from") LocalDateTime from,
            @Param("now") LocalDateTime now,
            @Param("afterId") Long afterId,
            Pageable pageable
    );

    @Transactional
    @Modifying
    @Query("UPDATE ScheduleAlarm sa SET sa.isSent = true WHERE sa.id = :id AND sa.isSent = false")
    int markAsSent(@Param("id") Long id);

    @Transactional
    @Modifying
    @Query("UPDATE ScheduleAlarm sa SET sa.failCount = sa.failCount + 1 WHERE sa.id = :id")
    void incrementFailCount(@Param("id") Long id);
}
