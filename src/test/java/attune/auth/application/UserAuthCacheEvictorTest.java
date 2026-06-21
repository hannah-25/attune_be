package attune.auth.application;

import attune.auth.domain.repository.UserAuthCacheRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class UserAuthCacheEvictorTest {

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void evictsImmediatelyWhenNoTransactionSynchronizationExists() {
        UserAuthCacheRepository repository = mock(UserAuthCacheRepository.class);
        UserAuthCacheEvictor evictor = new UserAuthCacheEvictor(repository);
        UUID userId = UUID.randomUUID();

        evictor.evictAfterCommit(userId);

        verify(repository).delete(userId);
    }

    @Test
    void evictsOnlyAfterCommitWhenTransactionSynchronizationExists() {
        UserAuthCacheRepository repository = mock(UserAuthCacheRepository.class);
        UserAuthCacheEvictor evictor = new UserAuthCacheEvictor(repository);
        UUID userId = UUID.randomUUID();
        TransactionSynchronizationManager.initSynchronization();

        evictor.evictAfterCommit(userId);

        verify(repository, never()).delete(userId);
        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
        synchronizations.forEach(TransactionSynchronization::afterCommit);
        verify(repository).delete(userId);
    }
}
