package attune.user.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record CreateUserRequest(
        @Schema(description = "닉네임", example = "홍길동", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 20, message = "이름은 2자 이상 20자 이하여야 합니다")
        String nickname,

        @Schema(description = "이메일", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이어야 합니다")
        String email,

        @Schema(description = "비밀번호 (8자 이상, 영문/숫자/특수문자 중 2가지 이상 조합)", example = "Abcd1234!", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "비밀번호는 필수입니다")
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
        @Pattern(
                regexp = "^(?=[\\x21-\\x7E]{8,}$)(?:(?=.*[A-Za-z])(?=.*\\d)|(?=.*[A-Za-z])(?=.*[^A-Za-z0-9])|(?=.*\\d)(?=.*[^A-Za-z0-9])).*$",
                message = "비밀번호는 8자 이상이며 영문, 숫자, 특수문자 중 2가지 이상을 포함해야 합니다"
        )
        String password,

        @AssertTrue(message = "이용약관에 동의해야 합니다.")
        @Schema(description = "이용약관 동의", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean termsOfService,

        @AssertTrue(message = "개인정보 처리방침에 동의해야 합니다.")
        @Schema(description = "개인정보 처리방침 동의(필수)", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean privacyPolicy,

        @Schema(description = "마케팅 정보 수신 동의(선택)", example = "false")
        boolean marketingConsent
) {}
