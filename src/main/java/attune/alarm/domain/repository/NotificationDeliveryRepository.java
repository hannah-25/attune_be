package attune.alarm.domain.repository;

import attune.alarm.domain.model.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, UUID> {

    Optional<NotificationDelivery> findByNotificationHistoryIdAndSubscriptionId(Long notificationHistoryId, Long subscriptionId);

    List<NotificationDelivery> findAllByNotificationHistoryIdIn(Collection<Long> notificationHistoryIds);
}
