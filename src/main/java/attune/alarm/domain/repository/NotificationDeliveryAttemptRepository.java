package attune.alarm.domain.repository;

import attune.alarm.domain.model.NotificationDeliveryAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationDeliveryAttemptRepository extends JpaRepository<NotificationDeliveryAttempt, UUID> {

    List<NotificationDeliveryAttempt> findAllByDeliveryId(UUID deliveryId);
}
