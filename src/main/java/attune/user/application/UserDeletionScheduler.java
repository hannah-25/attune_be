package attune.user.application;

import attune.user.domain.model.User;
import attune.user.domain.model.UserStatus;
import attune.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDeletionScheduler {

    private final UserRepository userRepository;
    private final UserPermanentDeletionService permanentDeletionService;
    private final Clock clock;

    @Value("${app.user.deletion-retention-days:30}")
    private int retentionDays;

    @Scheduled(cron = "${app.user.deletion-cron:0 0 3 * * *}")
    public void deleteExpiredWithdrawnUsers() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusDays(retentionDays);
        List<User> targets = userRepository.findExpiredWithdrawnUsers(UserStatus.WITHDRAWAL, cutoff);
        if (targets.isEmpty()) {
            return;
        }

        int deleted = 0;
        for (User user : targets) {
            try {
                if (permanentDeletionService.deleteExpiredWithdrawal(user.getId(), cutoff)) {
                    deleted++;
                }
            } catch (RuntimeException e) {
                log.error("[영구삭제] 실패 - userId={}", user.getId(), e);
            }
        }
        log.info("[영구삭제] 완료 - 대상={}명, 삭제={}명", targets.size(), deleted);
    }
}
