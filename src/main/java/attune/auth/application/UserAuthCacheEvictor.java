package attune.auth.application;

import attune.auth.domain.repository.UserAuthCacheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserAuthCacheEvictor {

    private final UserAuthCacheRepository userAuthCacheRepository;

    public void evictAfterCommit(UUID userId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            userAuthCacheRepository.delete(userId);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                userAuthCacheRepository.delete(userId);
            }
        });
    }
}
