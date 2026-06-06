package attune.alarm.domain.repository;

import attune.alarm.domain.model.NotificationAlarmType;
import attune.alarm.domain.model.NotificationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.UUID;

public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, Long> {

    boolean existsByUserIdAndAlarmTypeAndReferenceIdAndAlarmScheduledAt(
            UUID userId,
            NotificationAlarmType alarmType,
            Long referenceId,
            LocalDateTime alarmScheduledAt
    );
}
