package attune.user.application;

import attune.user.domain.model.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, atLeastOnce()).update(sqlCaptor.capture(), any(Object[].class));
        List<String> executedSql = sqlCaptor.getAllValues();
        assertThat(executedSql).contains(
                "DELETE FROM journal_tag_logs WHERE user_id = ?",
                "DELETE FROM user_journal_tag_preferences WHERE user_id = ?",
                "DELETE FROM journal_tags WHERE owner_user_id = ?"
        );
        assertThat(executedSql).noneMatch(sql ->
                sql.contains("condition_logs") || sql.contains("side_effect_logs") || sql.contains("trouble_logs")
        );
        verify(entityManager).remove(any(User.class));
    }
}
