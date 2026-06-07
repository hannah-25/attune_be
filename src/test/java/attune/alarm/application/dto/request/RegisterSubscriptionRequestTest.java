package attune.alarm.application.dto.request;

import attune.alarm.domain.model.NotificationPlatform;
import attune.alarm.domain.model.NotificationProvider;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisterSubscriptionRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void webPushRequiresEndpointP256dhAndAuth() {
        RegisterSubscriptionRequest request = new RegisterSubscriptionRequest(
                NotificationPlatform.WEB,
                NotificationProvider.WEB_PUSH,
                " ",
                null,
                "",
                null
        );

        Set<ConstraintViolation<RegisterSubscriptionRequest>> violations = validator.validate(request);

        assertEquals(Set.of("endpoint", "p256dh", "auth"), violationFields(violations));
    }

    @Test
    void fcmRequiresToken() {
        RegisterSubscriptionRequest request = new RegisterSubscriptionRequest(
                NotificationPlatform.ANDROID,
                NotificationProvider.FCM,
                null,
                null,
                null,
                " "
        );

        Set<ConstraintViolation<RegisterSubscriptionRequest>> violations = validator.validate(request);

        assertEquals(Set.of("token"), violationFields(violations));
    }

    @Test
    void apnsRequiresToken() {
        RegisterSubscriptionRequest request = new RegisterSubscriptionRequest(
                NotificationPlatform.IOS,
                NotificationProvider.APNS,
                null,
                null,
                null,
                null
        );

        Set<ConstraintViolation<RegisterSubscriptionRequest>> violations = validator.validate(request);

        assertEquals(Set.of("token"), violationFields(violations));
    }

    @Test
    void validProviderCredentialsPassValidation() {
        RegisterSubscriptionRequest webPush = new RegisterSubscriptionRequest(
                NotificationPlatform.WEB,
                NotificationProvider.WEB_PUSH,
                "https://push.example/subscription",
                "p256dh",
                "auth",
                null
        );
        RegisterSubscriptionRequest fcm = new RegisterSubscriptionRequest(
                NotificationPlatform.ANDROID,
                NotificationProvider.FCM,
                null,
                null,
                null,
                "fcm-token"
        );
        RegisterSubscriptionRequest apns = new RegisterSubscriptionRequest(
                NotificationPlatform.IOS,
                NotificationProvider.APNS,
                null,
                null,
                null,
                "apns-token"
        );

        assertTrue(validator.validate(webPush).isEmpty());
        assertTrue(validator.validate(fcm).isEmpty());
        assertTrue(validator.validate(apns).isEmpty());
    }

    private Set<String> violationFields(Set<ConstraintViolation<RegisterSubscriptionRequest>> violations) {
        return violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
    }
}
