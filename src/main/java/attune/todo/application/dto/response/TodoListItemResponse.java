package attune.todo.application.dto.response;

import attune.todo.domain.model.Todo;

import java.time.LocalDateTime;

public record TodoListItemResponse(
        Long todoId,
        String title,
        LocalDateTime dueAt,
        boolean isAllDay,
        boolean isCompleted
) {
    public static TodoListItemResponse from(Todo todo) {
        return new TodoListItemResponse(
                todo.getId(),
                todo.getTitle(),
                todo.getDueAt(),
                todo.isAllDay(),
                todo.isCompleted()
        );
    }
}
