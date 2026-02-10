package attune.user.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record CreateUserRequest(
        @Schema(description = "닉네임", example = "홍길동", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "닉네임은 필수입니다")
        @Size(min = 2, max = 20, message = "이름은 2자 이상 20자 이하여야 합니다")
        String nickname,

        @Schema(description = "이메일", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        String email,

        @Schema(description = "비밀번호 (대소문자/숫자/특수문자 포함 8~20자)", example = "Abcd1234!", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "비밀번호는 필수입니다")
        @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
                message = "비밀번호는 대소문자, 숫자, 특수문자를 포함해야 합니다")
        String password,

        @AssertTrue(message = "이용약관에 동의해야 합니다")
        @Schema(description = "이용약관 동의", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean termsOfService,

        @AssertTrue(message = "개인정보 처리방침에 동의해야 합니다")
        @Schema(description = "개인정보 처리방침 동의(필수)", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean privacyPolicy,

        @Schema(description = "마케팅 정보 수신 동의(선택)", example = "false")
        boolean marketingConsent
) {}
