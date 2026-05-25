package attune.todo.application.dto.request;

import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record UpdateTodoRequest(
        @Size(min = 1, max = 100) String title,
        LocalDateTime dueAt,
        Boolean isAllDay,
        Boolean isCompleted
) {}
