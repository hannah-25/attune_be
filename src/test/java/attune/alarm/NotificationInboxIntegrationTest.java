package attune.alarm;

import attune.alarm.domain.model.NotificationAlarmType;
import attune.alarm.domain.model.NotificationDelivery;
import attune.alarm.domain.model.NotificationHistory;
import attune.alarm.domain.model.NotificationStatus;
import attune.alarm.domain.repository.NotificationDeliveryRepository;
import attune.alarm.domain.repository.NotificationHistoryRepository;
import attune.support.IntegrationTest;
import attune.user.domain.model.User;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationInboxIntegrationTest extends IntegrationTest {

    @Autowired
    private NotificationHistoryRepository historyRepository;
    @Autowired
    private NotificationDeliveryRepository deliveryRepository;

    @Test
    void listsOnlyCurrentUsersNotificationsWithCursorReadFilterAndDeliverySummary() throws Exception {
        User user = testUsers.activeUser("notification-inbox@example.com");
        User otherUser = testUsers.activeUser("other-notification-inbox@example.com");
        LocalDateTime base = LocalDateTime.of(2026, 7, 24, 10, 0);

        NotificationHistory oldest = saveHistory(user, 1L, "oldest", base, base.plusMinutes(1));
        NotificationHistory newest = saveHistory(user, 2L, "newest", base.plusMinutes(2), null);
        saveHistory(otherUser, 3L, "other user's notification", base.plusMinutes(3), null);
        deliveryRepository.saveAndFlush(NotificationDelivery.builder()
                .notificationHistoryId(newest.getId())
                .subscriptionId(1L)
                .providerAcceptedAt(base.plusMinutes(2))
                .displayedAt(base.plusMinutes(3))
                .createdAt(base.plusMinutes(2))
                .updatedAt(base.plusMinutes(3))
                .build());

        String firstPage = mockMvc.perform(get("/v1/notifications")
                        .header("Authorization", testUsers.bearer(user))
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications[0].id").value(newest.getId()))
                .andExpect(jsonPath("$.notifications[0].url").value("/home"))
                .andExpect(jsonPath("$.notifications[0].deliveryStatus").value("DISPLAYED"))
                .andExpect(jsonPath("$.nextCursor").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String cursor = objectMapper.readTree(firstPage).path("nextCursor").asText();

        mockMvc.perform(get("/v1/notifications")
                        .header("Authorization", testUsers.bearer(user))
                        .param("size", "1")
                        .param("cursor", cursor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications[0].id").value(oldest.getId()))
                .andExpect(jsonPath("$.notifications[1]").doesNotExist())
                .andExpect(jsonPath("$.nextCursor").isEmpty());

        mockMvc.perform(get("/v1/notifications")
                        .header("Authorization", testUsers.bearer(user))
                        .param("status", "UNREAD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications[0].id").value(newest.getId()))
                .andExpect(jsonPath("$.notifications[1]").doesNotExist());

        mockMvc.perform(get("/v1/notifications")
                        .header("Authorization", testUsers.bearer(user))
                        .param("status", "READ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications[0].id").value(oldest.getId()))
                .andExpect(jsonPath("$.notifications[1]").doesNotExist());
    }

    @Test
    void marksOnlyOwnersNotificationAsReadAndIsIdempotent() throws Exception {
        User user = testUsers.activeUser("notification-read@example.com");
        User otherUser = testUsers.activeUser("other-notification-read@example.com");
        LocalDateTime now = LocalDateTime.of(2026, 7, 24, 10, 0);
        NotificationHistory ownHistory = saveHistory(user, 1L, "own", now, null);
        NotificationHistory otherHistory = saveHistory(otherUser, 2L, "other", now.plusMinutes(1), null);

        mockMvc.perform(patch("/v1/notifications/{notificationHistoryId}/read", ownHistory.getId())
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isNoContent());
        LocalDateTime firstReadAt = historyRepository.findById(ownHistory.getId()).orElseThrow().getReadAt();
        assertThat(firstReadAt).isNotNull();

        mockMvc.perform(patch("/v1/notifications/{notificationHistoryId}/read", ownHistory.getId())
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isNoContent());
        assertThat(historyRepository.findById(ownHistory.getId()).orElseThrow().getReadAt())
                .isEqualTo(firstReadAt);

        mockMvc.perform(patch("/v1/notifications/{notificationHistoryId}/read", otherHistory.getId())
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isNotFound());
        assertThat(historyRepository.findById(otherHistory.getId()).orElseThrow().getReadAt()).isNull();
    }

    private NotificationHistory saveHistory(
            User user,
            Long referenceId,
            String title,
            LocalDateTime sentAt,
            LocalDateTime readAt
    ) {
        return historyRepository.saveAndFlush(NotificationHistory.builder()
                .userId(user.getId())
                .alarmType(NotificationAlarmType.MEDICATION)
                .referenceId(referenceId)
                .alarmScheduledAt(sentAt)
                .title(title)
                .body(title + " body")
                .status(NotificationStatus.SENT)
                .sentAt(sentAt)
                .readAt(readAt)
                .build());
    }
}
