package attune.schedule.domain.application;

import attune.common.error.notfound.ScheduleCategoryNotFoundException;
import attune.common.util.SecurityUtils;
import attune.schedule.domain.application.dto.request.CreateCategoryRequest;
import attune.schedule.domain.application.dto.request.UpdateCategoryRequest;
import attune.schedule.domain.application.dto.response.CategoryListItemResponse;
import attune.schedule.domain.application.dto.response.CategoryListResponse;
import attune.schedule.domain.application.dto.response.CategoryResponse;
import attune.schedule.domain.model.ScheduleCategory;
import attune.schedule.domain.repository.ScheduleCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class ScheduleCategoryService {

    private final ScheduleCategoryRepository scheduleCategoryRepository;

    @Transactional(readOnly = true)
    public CategoryListResponse getCategories() {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        List<CategoryListItemResponse> items = scheduleCategoryRepository
                .findAllByUserIdAndIsActiveTrue(userId)
                .stream()
                .map(CategoryListItemResponse::from)
                .toList();
        return new CategoryListResponse(items);
    }

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        ScheduleCategory category = ScheduleCategory.builder()
                .userId(userId)
                .categoryName(request.categoryName())
                .color(request.color())
                .isActive(true)
                .build();
        return CategoryResponse.from(scheduleCategoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse updateCategory(Long categoryId, UpdateCategoryRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        ScheduleCategory category = scheduleCategoryRepository.findByIdAndIsActiveTrue(categoryId)
                .orElseThrow(ScheduleCategoryNotFoundException::new);
        if (!category.getUserId().equals(userId)) {
            throw new ScheduleCategoryNotFoundException();
        }
        category.update(request.categoryName(), request.color());
        return CategoryResponse.from(category);
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        ScheduleCategory category = scheduleCategoryRepository.findByIdAndIsActiveTrue(categoryId)
                .orElseThrow(ScheduleCategoryNotFoundException::new);
        if (!category.getUserId().equals(userId)) {
            throw new ScheduleCategoryNotFoundException();
        }
        category.deactivate();
    }
}
