package attune.user.application;

import attune.user.domain.model.User;
import attune.user.domain.model.UserStatus;
import attune.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDeletionScheduler {

    private final UserRepository userRepository;

    @Value("${app.user.deletion-retention-days:30}")
    private int retentionDays;

    // 매일 새벽 3시 실행
    @Scheduled(cron = "${app.user.deletion-cron:0 0 3 * * *}")
    @Transactional
    public void deleteExpiredWithdrawnUsers() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        List<User> targets = userRepository.findExpiredWithdrawnUsers(UserStatus.WITHDRAWAL, cutoff);

        if (targets.isEmpty()) {
            return;
        }

        for (User user : targets) {
            log.info("[영구삭제] userId={}, email={}, withdrawalAt={}",
                    user.getId(), user.getEmail(), user.getWithdrawalAt());
        }

        targets.forEach(user -> user.updateActiveStatus(false));
        log.info("[영구삭제] 완료 - 총 {}명", targets.size());
    }
}
