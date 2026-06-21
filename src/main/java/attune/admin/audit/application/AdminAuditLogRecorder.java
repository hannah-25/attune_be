package attune.admin.audit.application;

import java.util.UUID;

/**
 * Member administration code can record audit events without depending on a User/member entity.
 */
public interface AdminAuditLogRecorder {

    void recordWithdrawalCancelled(
            UUID memberId,
            String memberNickname,
            UUID adminId,
            String adminEmail,
            String reason
    );

    void recordMemberDeleted(
            UUID memberId,
            UUID adminId,
            String adminEmail,
            String reason
    );

    void recordMemberSoftDeleted(
            UUID memberId,
            UUID adminId,
            String adminEmail,
            String reason
    );

    void recordStatusChanged(
            UUID memberId,
            String memberNickname,
            UUID adminId,
            String adminEmail,
            String reason
    );
}
