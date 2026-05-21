package attune.communityBoard.application.dto.request;

import attune.communityBoard.domain.model.PostCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePostRequest(

        @Schema(description = "게시글 카테고리", example = "약물 정보", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        PostCategory postCategory,

        @Schema(description = "게시글 제목", example = "콘서타 18mg 복용 7일차입니다", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String title,

        @Schema(description = "게시글 본문", example = "약을 먹은지 일주일이 되었는데도 별 느낌이 없어요. 용량을 올려야 할까요?", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        String content,

        @Schema(description = "익명 여부", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        boolean isAnonymous
) {
}