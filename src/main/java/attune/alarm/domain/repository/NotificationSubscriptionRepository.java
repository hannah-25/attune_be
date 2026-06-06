package attune.alarm.domain.repository;

import attune.alarm.domain.model.NotificationSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationSubscriptionRepository extends JpaRepository<NotificationSubscription, Long> {

    List<NotificationSubscription> findAllByUserIdAndEnabledTrue(UUID userId);

    Optional<NotificationSubscription> findByUserIdAndEndpoint(UUID userId, String endpoint);

    Optional<NotificationSubscription> findByUserIdAndToken(UUID userId, String token);
}
