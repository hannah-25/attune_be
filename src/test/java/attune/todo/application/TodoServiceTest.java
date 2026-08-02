package attune.todo.application;

import attune.common.error.BadRequestException;
import attune.common.security.CustomUserDetails;
import attune.todo.domain.model.Todo;
import attune.todo.domain.repository.TodoRepository;
import attune.user.domain.model.UserStatus;
import attune.user.domain.model.UserType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TodoServiceTest {

    private final TodoRepository todoRepository = mock(TodoRepository.class);
    private final TodoService todoService = new TodoService(todoRepository);
    private final UUID userId = UUID.randomUUID();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getsTodosInInclusiveDateRange() {
        LocalDate startDate = LocalDate.of(2026, 8, 1);
        LocalDate endDate = LocalDate.of(2026, 8, 7);
        authenticate();
        when(todoRepository.findAllByDate(userId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay()))
                .thenReturn(List.of(Todo.builder()
                        .id(1L).userId(userId).text("todo").dueAt(LocalDateTime.of(2026, 8, 7, 23, 59))
                        .isAllDay(false).isCompleted(false).isDeleted(false).createdAt(LocalDateTime.now()).build()));

        var response = todoService.getTodosByDateRange(startDate, endDate);

        assertEquals(1, response.todos().size());
        assertEquals(1L, response.todos().get(0).todoId());
        verify(todoRepository).findAllByDate(userId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
    }

    @Test
    void rejectsReversedDateRange() {
        assertThrows(BadRequestException.class, () -> todoService.getTodosByDateRange(
                LocalDate.of(2026, 8, 8), LocalDate.of(2026, 8, 7)));
    }

    private void authenticate() {
        CustomUserDetails principal = CustomUserDetails.fromJwt(userId, UserType.USER, UserStatus.ACTIVE);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }
}
