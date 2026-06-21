package attune.admin.member.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelWithdrawalRequest(
        @NotBlank(message = "처리 사유는 필수입니다.")
        @Size(min = 5, max = 1000, message = "처리 사유는 공백을 제외하고 5자 이상 1000자 이하여야 합니다.")
        String reason
) {
    public CancelWithdrawalRequest {
        if (reason != null) {
            reason = reason.trim();
        }
    }
}
