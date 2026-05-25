package attune.todo.application.dto.response;

import attune.todo.domain.model.Todo;

import java.time.LocalDateTime;

public record TodoDetailResponse(
        Long todoId,
        String title,
        LocalDateTime dueAt,
        boolean isAllDay,
        boolean isCompleted,
        LocalDateTime createdAt
) {
    public static TodoDetailResponse from(Todo todo) {
        return new TodoDetailResponse(
                todo.getId(),
                todo.getTitle(),
                todo.getDueAt(),
                todo.isAllDay(),
                todo.isCompleted(),
                todo.getCreatedAt()
        );
    }
}
