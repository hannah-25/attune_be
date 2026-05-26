package attune.support.application.dto.request;

import attune.support.domain.model.SupportInquiryType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSupportInquiryRequest(
        @NotNull SupportInquiryType type,
        @NotBlank @Size(max = 100) String title,
        @NotBlank @Size(max = 5000) String content,
        @NotBlank @Email @Size(max = 255) String email
) {
}
