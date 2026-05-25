package attune.todo.application.dto.response;

import attune.todo.domain.model.Todo;

public record UpdateTodoResponse(
        Long todoId,
        String text
) {
    public static UpdateTodoResponse from(Todo todo) {
        return new UpdateTodoResponse(todo.getId(), todo.getText());
    }
}
