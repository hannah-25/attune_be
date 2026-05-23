package attune.schedule.adapter.web;

import attune.schedule.application.ScheduleCategoryService;
import attune.schedule.application.dto.request.CreateCategoryRequest;
import attune.schedule.application.dto.request.UpdateCategoryRequest;
import attune.schedule.application.dto.response.CategoryListResponse;
import attune.schedule.application.dto.response.CategoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "일정 카테고리", description = "일정 카테고리(라벨) 관리 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/schedule-categories")
public class ScheduleCategoryController {

    private final ScheduleCategoryService scheduleCategoryService;

    @Operation(summary = "일정 카테고리 목록 조회", description = "현재 로그인한 사용자가 생성한 활성 카테고리만 반환한다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    public ResponseEntity<CategoryListResponse> getCategories() {
        return ResponseEntity.ok(scheduleCategoryService.getCategories());
    }

    @Operation(summary = "일정 카테고리 생성")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleCategoryService.createCategory(request));
    }

    @Operation(summary = "일정 카테고리 수정")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "404", description = "카테고리 없음")
    })
    @PatchMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        return ResponseEntity.ok(scheduleCategoryService.updateCategory(categoryId, request));
    }

    @Operation(summary = "일정 카테고리 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "카테고리 없음")
    })
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long categoryId) {
        scheduleCategoryService.deleteCategory(categoryId);
        return ResponseEntity.noContent().build();
    }
}
