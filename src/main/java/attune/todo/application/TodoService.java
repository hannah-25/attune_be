package attune.todo.application;

import attune.common.error.BadRequestException;
import attune.common.error.badrequest.NoFieldToUpdateException;
import attune.common.error.notfound.TodoNotFoundException;
import attune.common.util.SecurityUtils;
import attune.todo.application.dto.request.CreateTodoRequest;
import attune.todo.application.dto.request.UpdateTodoRequest;
import attune.todo.application.dto.response.TodoDetailResponse;
import attune.todo.application.dto.response.TodoListItemResponse;
import attune.todo.application.dto.response.TodoListResponse;
import attune.todo.application.dto.response.UpdateTodoResponse;
import attune.todo.domain.model.Todo;
import attune.todo.domain.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class TodoService {

    private final TodoRepository todoRepository;

    @Transactional
    public void createTodo(CreateTodoRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();

        Todo todo = Todo.builder()
                .userId(userId)
                .text(request.text())
                .dueAt(request.dueAt())
                .isAllDay(request.isAllDay())
                .isCompleted(false)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .build();

        todoRepository.save(todo);
    }

    @Transactional(readOnly = true)
    public TodoDetailResponse getTodo(Long todoId) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        Todo todo = todoRepository.findByIdAndUserIdAndIsDeletedFalse(todoId, userId)
                .orElseThrow(TodoNotFoundException::new);
        return TodoDetailResponse.from(todo);
    }

    @Transactional(readOnly = true)
    public TodoListResponse getTodosByDate(LocalDate date) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        LocalDateTime startDate = date.atStartOfDay();
        LocalDateTime endDate = date.plusDays(1).atStartOfDay();

        List<TodoListItemResponse> todos = todoRepository.findAllByDate(userId, startDate, endDate)
                .stream()
                .map(TodoListItemResponse::from)
                .toList();

        return new TodoListResponse(todos);
    }

    @Transactional
    public UpdateTodoResponse updateTodo(Long todoId, UpdateTodoRequest request) {
        if (request.text() == null && request.dueAt() == null
                && request.isAllDay() == null && request.isCompleted() == null) {
            throw new NoFieldToUpdateException();
        }

        if (request.text() != null && request.text().isBlank()) {
            throw new BadRequestException("할 일 내용은 공백만 입력할 수 없습니다.");
        }

        UUID userId = SecurityUtils.getCurrentUserUuid();
        Todo todo = todoRepository.findByIdAndUserIdAndIsDeletedFalse(todoId, userId)
                .orElseThrow(TodoNotFoundException::new);

        LocalDateTime dueAt = request.dueAt();
        if (Boolean.TRUE.equals(request.isAllDay()) && dueAt != null) {
            dueAt = dueAt.toLocalDate().atStartOfDay();
        }

        todo.update(request.text(), dueAt, request.isAllDay(), request.isCompleted());

        return UpdateTodoResponse.from(todo);
    }

}
