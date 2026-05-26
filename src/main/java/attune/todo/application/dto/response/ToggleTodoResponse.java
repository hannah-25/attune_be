package attune.todo.application.dto.response;

import attune.todo.domain.model.Todo;

public record ToggleTodoResponse(
        Long todoId,
        boolean isCompleted
) {
    public static ToggleTodoResponse from(Todo todo) {
        return new ToggleTodoResponse(todo.getId(), todo.isCompleted());
    }
}
