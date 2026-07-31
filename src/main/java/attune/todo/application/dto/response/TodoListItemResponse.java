package attune.todo.application.dto.response;

import attune.todo.domain.model.Todo;

import java.time.LocalDateTime;

public record TodoListItemResponse(
        Long todoId,
        String text,
        LocalDateTime dueAt,
        boolean isAllDay,
        boolean isCompleted
) {
    public static TodoListItemResponse from(Todo todo) {
        return new TodoListItemResponse(
                todo.getId(),
                todo.getText(),
                todo.getDueAt(),
                todo.isAllDay(),
                todo.isCompleted()
        );
    }
}
