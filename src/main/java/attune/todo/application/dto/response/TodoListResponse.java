package attune.todo.application.dto.response;

import java.util.List;

public record TodoListResponse(
        List<TodoListItemResponse> todos
) {}
