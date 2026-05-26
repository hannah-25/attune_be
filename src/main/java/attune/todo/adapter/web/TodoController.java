package attune.todo.adapter.web;

import attune.common.ApiVersion;
import attune.todo.application.TodoService;
import attune.todo.application.dto.request.CreateTodoRequest;
import attune.todo.application.dto.request.UpdateTodoRequest;
import attune.todo.application.dto.response.TodoDetailResponse;
import attune.todo.application.dto.response.TodoListResponse;
import attune.todo.application.dto.response.UpdateTodoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "할 일", description = "할 일 관리 API")
@RequiredArgsConstructor
@RestController
@RequestMapping(ApiVersion.V1 + "/todos")
public class TodoController {

    private final TodoService todoService;

    @Operation(summary = "할 일 생성")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @PostMapping
    public ResponseEntity<Void> createTodo(@Valid @RequestBody CreateTodoRequest request) {
        todoService.createTodo(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "특정 일자 할 일 목록 조회")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    public ResponseEntity<TodoListResponse> getTodosByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(todoService.getTodosByDate(date));
    }

    @Operation(summary = "할 일 상세 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "할 일 없음")
    })
    @GetMapping("/{todoId}")
    public ResponseEntity<TodoDetailResponse> getTodo(@PathVariable Long todoId) {
        return ResponseEntity.ok(todoService.getTodo(todoId));
    }

    @Operation(summary = "할 일 수정")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "404", description = "할 일 없음")
    })
    @PatchMapping("/{todoId}")
    public ResponseEntity<UpdateTodoResponse> updateTodo(
            @PathVariable Long todoId,
            @Valid @RequestBody UpdateTodoRequest request
    ) {
        return ResponseEntity.ok(todoService.updateTodo(todoId, request));
    }

}
