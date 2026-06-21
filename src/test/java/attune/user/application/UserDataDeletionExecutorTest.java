package attune.user.application;

import attune.user.domain.model.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserDataDeletionExecutorTest {

    @Test
    void deletesChildrenBeforeUserAndFlushes() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        EntityManager entityManager = mock(EntityManager.class);
        UUID userId = UUID.randomUUID();
        when(entityManager.find(User.class, userId)).thenReturn(mock(User.class));
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

        UserDataDeletionExecutor executor = new UserDataDeletionExecutor(jdbcTemplate, entityManager);

        executor.deleteAllUserData(userId);

        verify(entityManager, times(2)).flush();
        verify(jdbcTemplate, atLeastOnce()).update(anyString(), any(Object[].class));
        verify(entityManager).remove(any(User.class));
    }
}
