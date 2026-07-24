package attune.alarm.application;

import attune.alarm.application.dto.response.NotificationDeliveryStatus;
import attune.alarm.application.dto.response.NotificationInboxItemResponse;
import attune.alarm.application.dto.response.NotificationInboxResponse;
import attune.alarm.domain.model.NotificationDelivery;
import attune.alarm.domain.model.NotificationHistory;
import attune.alarm.domain.model.NotificationReadStatus;
import attune.alarm.domain.repository.NotificationDeliveryRepository;
import attune.alarm.domain.repository.NotificationHistoryRepository;
import attune.common.error.BadRequestException;
import attune.common.error.notfound.NotificationHistoryNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Transactional
public class NotificationInboxService {

    private static final int MAX_CURSOR_LENGTH = 512;

    private final NotificationHistoryRepository historyRepository;
    private final NotificationDeliveryRepository deliveryRepository;

    @Transactional(readOnly = true)
    public NotificationInboxResponse getInbox(
            UUID userId,
            NotificationReadStatus status,
            String cursor,
            int size
    ) {
        Cursor decodedCursor = Cursor.decode(cursor);
        Boolean unread = status == null ? null : status == NotificationReadStatus.UNREAD;
        List<NotificationHistory> histories = historyRepository.findInbox(
                userId,
                unread,
                decodedCursor == null ? null : decodedCursor.sentAt(),
                decodedCursor == null ? null : decodedCursor.id(),
                PageRequest.of(0, size + 1)
        );

        boolean hasNext = histories.size() > size;
        List<NotificationHistory> page = hasNext ? histories.subList(0, size) : histories;
        Map<Long, NotificationDeliveryStatus> deliveryStatuses = findDeliveryStatuses(page);
        List<NotificationInboxItemResponse> notifications = page.stream()
                .map(history -> NotificationInboxItemResponse.from(history, deliveryStatuses.get(history.getId())))
                .toList();

        String nextCursor = hasNext ? Cursor.encode(page.get(page.size() - 1)) : null;
        return new NotificationInboxResponse(notifications, nextCursor);
    }

    public void markAsRead(UUID userId, Long notificationHistoryId) {
        if (!historyRepository.existsByIdAndUserId(notificationHistoryId, userId)) {
            throw new NotificationHistoryNotFoundException();
        }
        historyRepository.markAsReadIfUnread(notificationHistoryId, userId, LocalDateTime.now());
    }

    private Map<Long, NotificationDeliveryStatus> findDeliveryStatuses(Collection<NotificationHistory> histories) {
        if (histories.isEmpty()) {
            return Map.of();
        }
        return deliveryRepository.findAllByNotificationHistoryIdIn(
                        histories.stream().map(NotificationHistory::getId).toList()
                ).stream()
                .filter(delivery -> statusOf(delivery) != null)
                .collect(Collectors.toMap(
                        NotificationDelivery::getNotificationHistoryId,
                        this::statusOf,
                        BinaryOperator.maxBy(Comparator.comparingInt(Enum::ordinal))
                ));
    }

    private NotificationDeliveryStatus statusOf(NotificationDelivery delivery) {
        if (delivery.getOpenedAt() != null) {
            return NotificationDeliveryStatus.OPENED;
        }
        if (delivery.getDisplayedAt() != null) {
            return NotificationDeliveryStatus.DISPLAYED;
        }
        if (delivery.getReceivedAt() != null) {
            return NotificationDeliveryStatus.RECEIVED;
        }
        if (delivery.getProviderAcceptedAt() != null) {
            return NotificationDeliveryStatus.PROVIDER_ACCEPTED;
        }
        return null;
    }

    private record Cursor(LocalDateTime sentAt, Long id) {

        private static String encode(NotificationHistory history) {
            String value = history.getSentAt() + "|" + history.getId();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
        }

        private static Cursor decode(String encoded) {
            if (encoded == null || encoded.isBlank()) {
                return null;
            }
            if (encoded.length() > MAX_CURSOR_LENGTH) {
                throw new BadRequestException("cursor 형식이 올바르지 않습니다.");
            }
            try {
                String value = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
                int separator = value.lastIndexOf('|');
                if (separator <= 0 || separator == value.length() - 1) {
                    throw new IllegalArgumentException();
                }
                LocalDateTime sentAt = LocalDateTime.parse(value.substring(0, separator));
                long id = Long.parseLong(value.substring(separator + 1));
                if (id <= 0) {
                    throw new IllegalArgumentException();
                }
                return new Cursor(sentAt, id);
            } catch (IllegalArgumentException | DateTimeParseException e) {
                throw new BadRequestException("cursor 형식이 올바르지 않습니다.");
            }
        }
    }
}
