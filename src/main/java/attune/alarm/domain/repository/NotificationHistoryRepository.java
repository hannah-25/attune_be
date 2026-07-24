package attune.alarm.domain.repository;

import attune.alarm.domain.model.NotificationHistory;
import attune.alarm.domain.model.NotificationAlarmType;
import attune.alarm.domain.model.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, Long> {

    @Query("""
            SELECT h FROM NotificationHistory h
            WHERE h.userId = :userId
              AND h.alarmType = :alarmType
              AND (h.referenceId = :referenceId OR (h.referenceId IS NULL AND :referenceId IS NULL))
              AND h.alarmScheduledAt = :scheduledAt
            """)
    Optional<NotificationHistory> findHistory(
            @Param("userId") UUID userId,
            @Param("alarmType") NotificationAlarmType alarmType,
            @Param("referenceId") Long referenceId,
            @Param("scheduledAt") LocalDateTime scheduledAt
    );

    @Modifying
    @Query("""
            UPDATE NotificationHistory h
            SET h.status = :sending, h.sentAt = :claimedAt
            WHERE h.userId = :userId
              AND h.alarmType = :alarmType
              AND (h.referenceId = :referenceId OR (h.referenceId IS NULL AND :referenceId IS NULL))
              AND h.alarmScheduledAt = :scheduledAt
              AND (h.status = :failed OR (h.status = :sending AND h.sentAt <= :staleBefore))
            """)
    int reclaimForRetry(
            @Param("userId") UUID userId,
            @Param("alarmType") NotificationAlarmType alarmType,
            @Param("referenceId") Long referenceId,
            @Param("scheduledAt") LocalDateTime scheduledAt,
            @Param("failed") NotificationStatus failed,
            @Param("sending") NotificationStatus sending,
            @Param("claimedAt") LocalDateTime claimedAt,
            @Param("staleBefore") LocalDateTime staleBefore
    );

    @Modifying
    @Query("""
            UPDATE NotificationHistory h SET h.status = :status
            WHERE h.id = :id
              AND h.status = :sending
              AND h.sentAt = :claimedAt
            """)
    int updateStatus(
            @Param("id") Long id,
            @Param("claimedAt") LocalDateTime claimedAt,
            @Param("sending") NotificationStatus sending,
            @Param("status") NotificationStatus status
    );

    @Query("""
            SELECT h FROM NotificationHistory h
            WHERE h.userId = :userId
              AND (:unread IS NULL
                   OR (:unread = true AND h.readAt IS NULL)
                   OR (:unread = false AND h.readAt IS NOT NULL))
              AND (:cursorSentAt IS NULL
                   OR h.sentAt < :cursorSentAt
                   OR (h.sentAt = :cursorSentAt AND h.id < :cursorId))
            ORDER BY h.sentAt DESC, h.id DESC
            """)
    List<NotificationHistory> findInbox(
            @Param("userId") UUID userId,
            @Param("unread") Boolean unread,
            @Param("cursorSentAt") LocalDateTime cursorSentAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    boolean existsByIdAndUserId(Long id, UUID userId);

    @Modifying
    @Query("""
            UPDATE NotificationHistory h
            SET h.readAt = :readAt
            WHERE h.id = :id
              AND h.userId = :userId
              AND h.readAt IS NULL
            """)
    int markAsReadIfUnread(
            @Param("id") Long id,
            @Param("userId") UUID userId,
            @Param("readAt") LocalDateTime readAt
    );
}
