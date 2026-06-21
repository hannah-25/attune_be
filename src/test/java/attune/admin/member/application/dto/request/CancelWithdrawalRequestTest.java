package attune.admin.member.application.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CancelWithdrawalRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void trimsReasonBeforeValidationAndUse() {
        CancelWithdrawalRequest request = new CancelWithdrawalRequest("  회원 본인 복구 요청  ");

        assertThat(request.reason()).isEqualTo("회원 본인 복구 요청");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsReasonShorterThanFiveCharactersAfterTrim() {
        CancelWithdrawalRequest request = new CancelWithdrawalRequest("  사유  ");

        assertThat(validator.validate(request)).isNotEmpty();
    }
}
