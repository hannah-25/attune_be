package attune.alarm;

import attune.alarm.application.ReceiptTokenHasher;
import attune.alarm.domain.model.NotificationDelivery;
import attune.alarm.domain.model.NotificationDeliveryAttempt;
import attune.alarm.domain.repository.NotificationDeliveryAttemptRepository;
import attune.alarm.domain.repository.NotificationDeliveryRepository;
import attune.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationDeliveryEventIntegrationTest extends IntegrationTest {

    @Autowired
    private NotificationDeliveryRepository deliveryRepository;
    @Autowired
    private NotificationDeliveryAttemptRepository attemptRepository;

    @Test
    void recordsFirstReceivedEventWithoutAuthentication() throws Exception {
        UUID deliveryId = saveDelivery();
        String token = "test-receipt-token";
        UUID attemptId = saveAttempt(deliveryId, token, LocalDateTime.now().plusHours(1));

        mockMvc.perform(post("/v1/notification-delivery-attempts/{id}/events", attemptId)
                        .contentType(APPLICATION_JSON)
                        .content(eventBody("RECEIVED", token)))
                .andExpect(status().isNoContent());

        NotificationDeliveryAttempt attempt = attemptRepository.findById(attemptId).orElseThrow();
        assertThat(attempt.getReceivedAt()).isNotNull();
        assertThat(attempt.getDisplayedAt()).isNull();
        NotificationDelivery delivery = deliveryRepository.findById(deliveryId).orElseThrow();
        assertThat(delivery.getReceivedAt()).isNotNull();
    }

    @Test
    void duplicateEventDoesNotChangeOriginalTimestamp() throws Exception {
        UUID deliveryId = saveDelivery();
        String token = "dup-token";
        UUID attemptId = saveAttempt(deliveryId, token, LocalDateTime.now().plusHours(1));

        mockMvc.perform(post("/v1/notification-delivery-attempts/{id}/events", attemptId)
                        .contentType(APPLICATION_JSON)
                        .content(eventBody("RECEIVED", token)))
                .andExpect(status().isNoContent());
        LocalDateTime firstReceivedAt = attemptRepository.findById(attemptId).orElseThrow().getReceivedAt();

        mockMvc.perform(post("/v1/notification-delivery-attempts/{id}/events", attemptId)
                        .contentType(APPLICATION_JSON)
                        .content(eventBody("RECEIVED", token)))
                .andExpect(status().isNoContent());

        assertThat(attemptRepository.findById(attemptId).orElseThrow().getReceivedAt())
                .isEqualTo(firstReceivedAt);
    }

    @Test
    void openedBeforeReceivedBackfillsEarlierStagesWithSameTimestamp() throws Exception {
        UUID deliveryId = saveDelivery();
        String token = "backfill-token";
        UUID attemptId = saveAttempt(deliveryId, token, LocalDateTime.now().plusHours(1));

        mockMvc.perform(post("/v1/notification-delivery-attempts/{id}/events", attemptId)
                        .contentType(APPLICATION_JSON)
                        .content(eventBody("OPENED", token)))
                .andExpect(status().isNoContent());

        NotificationDeliveryAttempt attempt = attemptRepository.findById(attemptId).orElseThrow();
        assertThat(attempt.getReceivedAt()).isEqualTo(attempt.getOpenedAt());
        assertThat(attempt.getDisplayedAt()).isEqualTo(attempt.getOpenedAt());
        assertThat(attempt.getOpenedAt()).isNotNull();
    }

    @Test
    void nonExistentAttemptReturnsNoContent() throws Exception {
        mockMvc.perform(post("/v1/notification-delivery-attempts/{id}/events", UUID.randomUUID())
                        .contentType(APPLICATION_JSON)
                        .content(eventBody("RECEIVED", "whatever")))
                .andExpect(status().isNoContent());
    }

    @Test
    void tokenMismatchReturnsNoContentAndDoesNotRecord() throws Exception {
        UUID deliveryId = saveDelivery();
        UUID attemptId = saveAttempt(deliveryId, "real-token", LocalDateTime.now().plusHours(1));

        mockMvc.perform(post("/v1/notification-delivery-attempts/{id}/events", attemptId)
                        .contentType(APPLICATION_JSON)
                        .content(eventBody("RECEIVED", "wrong-token")))
                .andExpect(status().isNoContent());

        assertThat(attemptRepository.findById(attemptId).orElseThrow().getReceivedAt()).isNull();
    }

    @Test
    void expiredTokenReturnsNoContentAndDoesNotRecord() throws Exception {
        UUID deliveryId = saveDelivery();
        String token = "expired-token";
        UUID attemptId = saveAttempt(deliveryId, token, LocalDateTime.now().minusMinutes(1));

        mockMvc.perform(post("/v1/notification-delivery-attempts/{id}/events", attemptId)
                        .contentType(APPLICATION_JSON)
                        .content(eventBody("RECEIVED", token)))
                .andExpect(status().isNoContent());

        assertThat(attemptRepository.findById(attemptId).orElseThrow().getReceivedAt()).isNull();
    }

    @Test
    void unknownEventValueReturnsNoContentAndDoesNotRecord() throws Exception {
        UUID deliveryId = saveDelivery();
        String token = "unknown-event-token";
        UUID attemptId = saveAttempt(deliveryId, token, LocalDateTime.now().plusHours(1));

        mockMvc.perform(post("/v1/notification-delivery-attempts/{id}/events", attemptId)
                        .contentType(APPLICATION_JSON)
                        .content(eventBody("CLICKED", token)))
                .andExpect(status().isNoContent());

        assertThat(attemptRepository.findById(attemptId).orElseThrow().getReceivedAt()).isNull();
    }

    @Test
    void malformedRequestReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/v1/notification-delivery-attempts/{id}/events", UUID.randomUUID())
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exceedingPerAttemptRateLimitReturnsTooManyRequests() throws Exception {
        UUID deliveryId = saveDelivery();
        String token = "rate-limit-token";
        UUID attemptId = saveAttempt(deliveryId, token, LocalDateTime.now().plusHours(1));

        for (int i = 0; i < 20; i++) {
            mockMvc.perform(post("/v1/notification-delivery-attempts/{id}/events", attemptId)
                            .contentType(APPLICATION_JSON)
                            .content(eventBody("RECEIVED", token)))
                    .andExpect(status().isNoContent());
        }

        mockMvc.perform(post("/v1/notification-delivery-attempts/{id}/events", attemptId)
                        .contentType(APPLICATION_JSON)
                        .content(eventBody("RECEIVED", token)))
                .andExpect(status().isTooManyRequests());
    }

    private UUID saveDelivery() {
        LocalDateTime now = LocalDateTime.now();
        return deliveryRepository.saveAndFlush(NotificationDelivery.builder()
                .notificationHistoryId(1L)
                .subscriptionId(1L)
                .createdAt(now)
                .updatedAt(now)
                .build()).getId();
    }

    private UUID saveAttempt(UUID deliveryId, String receiptToken, LocalDateTime expiresAt) {
        return attemptRepository.saveAndFlush(NotificationDeliveryAttempt.builder()
                .deliveryId(deliveryId)
                .attemptNo(1)
                .receiptTokenHash(ReceiptTokenHasher.hash(receiptToken))
                .receiptExpiresAt(expiresAt)
                .createdAt(LocalDateTime.now())
                .build()).getId();
    }

    private String eventBody(String event, String receiptToken) throws Exception {
        return objectMapper.writeValueAsString(Map.of("event", event, "receiptToken", receiptToken));
    }
}
