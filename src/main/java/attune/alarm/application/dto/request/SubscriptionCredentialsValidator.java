package attune.alarm.application.dto.request;

import attune.alarm.domain.model.NotificationProvider;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SubscriptionCredentialsValidator
        implements ConstraintValidator<ValidSubscriptionCredentials, RegisterSubscriptionRequest> {

    @Override
    public boolean isValid(RegisterSubscriptionRequest request, ConstraintValidatorContext context) {
        if (request == null || request.provider() == null) {
            return true;
        }

        return switch (request.provider()) {
            case WEB_PUSH -> validateWebPush(request, context);
            case FCM, APNS -> validateToken(request, context);
        };
    }

    private boolean validateWebPush(RegisterSubscriptionRequest request, ConstraintValidatorContext context) {
        boolean valid = true;
        context.disableDefaultConstraintViolation();

        valid &= addViolationIfBlank(request.endpoint(), "endpoint", context);
        valid &= addViolationIfBlank(request.p256dh(), "p256dh", context);
        valid &= addViolationIfBlank(request.auth(), "auth", context);

        return valid;
    }

    private boolean validateToken(RegisterSubscriptionRequest request, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        return addViolationIfBlank(request.token(), "token", context);
    }

    private boolean addViolationIfBlank(
            String value,
            String field,
            ConstraintValidatorContext context
    ) {
        if (value != null && !value.isBlank()) {
            return true;
        }

        context.buildConstraintViolationWithTemplate(
                        field + "는 provider에 필수입니다."
                )
                .addPropertyNode(field)
                .addConstraintViolation();
        return false;
    }
}
