package attune.todo.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateTodoRequest(
        @NotBlank @Size(max = 100) String title,
        @NotNull LocalDateTime dueAt,
        boolean isAllDay
) {}
