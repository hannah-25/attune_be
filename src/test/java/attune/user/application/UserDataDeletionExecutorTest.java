package attune.user.application;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserDataDeletionExecutorTest {

    @Test
    void deletesChildrenBeforeUserAndClearsPersistenceContext() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        EntityManager entityManager = mock(EntityManager.class);
        UUID userId = UUID.randomUUID();
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

        UserDataDeletionExecutor executor = new UserDataDeletionExecutor(jdbcTemplate, entityManager);

        executor.deleteAllUserData(userId);

        verify(entityManager).flush();
        verify(jdbcTemplate, atLeastOnce()).update(anyString(), any(Object[].class));
        verify(entityManager).clear();
    }
}
