package attune.user.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(

        @NotBlank(message = "현재 비밀번호는 필수입니다")
        String currentPassword,

        @NotBlank(message = "새 비밀번호는 필수입니다")
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
        @Pattern(
                regexp = "^(?=[\\x21-\\x7E]{8,}$)(?:(?=.*[A-Za-z])(?=.*\\d)|(?=.*[A-Za-z])(?=.*[^A-Za-z0-9])|(?=.*\\d)(?=.*[^A-Za-z0-9])).*$",
                message = "비밀번호는 8자 이상이며 영문, 숫자, 특수문자 중 2가지 이상을 포함해야 합니다"
        )
        String newPassword
) {}
