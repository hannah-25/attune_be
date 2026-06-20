package attune.user.application;

import attune.todo.domain.model.Todo;
import attune.todo.domain.repository.TodoRepository;
import attune.user.domain.model.User;
import attune.user.domain.model.UserStatus;
import attune.user.domain.model.UserType;
import attune.user.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(UserDataDeletionExecutor.class)
class UserDataDeletionExecutorIntegrationTest {

    @Autowired
    private UserDataDeletionExecutor executor;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TodoRepository todoRepository;

    @Test
    void allDeletionStatementsMatchTheCurrentSchema() {
        assertThatThrownBy(() -> executor.deleteAllUserData(UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("회원 삭제 대상이 존재하지 않습니다.");
    }

    @Test
    void deletesUserAndOwnedData() {
        User user = userRepository.save(User.builder()
                .email("delete-me@example.com")
                .nickname("delete-me")
                .userType(UserType.USER)
                .userStatus(UserStatus.WITHDRAWAL)
                .withdrawalAt(LocalDateTime.now().minusDays(31))
                .build());
        todoRepository.save(Todo.builder()
                .userId(user.getId())
                .text("delete")
                .dueAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build());

        executor.deleteAllUserData(user.getId());

        assertThat(userRepository.findById(user.getId())).isEmpty();
        assertThat(todoRepository.findAll()).isEmpty();
    }
}
