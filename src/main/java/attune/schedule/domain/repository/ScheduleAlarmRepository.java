package attune.schedule.domain.repository;

import attune.schedule.domain.model.ScheduleAlarm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleAlarmRepository extends JpaRepository<ScheduleAlarm, Long> {

    List<ScheduleAlarm> findAllByScheduleId(Long scheduleId);

    @Modifying
    @Query("DELETE FROM ScheduleAlarm sa WHERE sa.schedule.id = :scheduleId")
    void deleteAllByScheduleId(@Param("scheduleId") Long scheduleId);

    @Query("""
            SELECT sa FROM ScheduleAlarm sa
            JOIN FETCH sa.schedule s
            WHERE sa.alarmAt = :alarmAt
              AND s.isDeleted = false
            """)
    List<ScheduleAlarm> findWithScheduleByAlarmAt(@Param("alarmAt") LocalDateTime alarmAt);
}
