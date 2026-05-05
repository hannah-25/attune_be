package attune.communityBoard.domain.application;

import attune.communityBoard.domain.model.PostCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdatePostRequest(

        @Schema(description = "게시글 카테고리")
        @NotNull
        PostCategory postCategory,

        @Schema(description = "게시글 제목")
        @NotBlank
        String title,

        @Schema(description = "게시글 본문")
        @NotNull
        String content
) {
}
