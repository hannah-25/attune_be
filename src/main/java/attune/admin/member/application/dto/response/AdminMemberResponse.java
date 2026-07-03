package attune.admin.member.application.dto.response;

import attune.user.domain.model.OAuthProvider;
import attune.user.domain.model.User;
import attune.user.domain.model.UserStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public record AdminMemberResponse(
        UUID id,
        String email,
        String nickname,
        UserStatus status,
        OAuthProvider provider,
        Instant createdAt,
        Instant lastLoginAt,
        Instant withdrawalRequestedAt,
        Instant withdrawalScheduledAt
) {
    public static AdminMemberResponse from(User user, int withdrawalRetentionDays) {
        LocalDateTime withdrawalRequestedAt = user.getWithdrawalAt();
        return new AdminMemberResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getUserStatus(),
                user.getProvider(),
                toInstant(user.getCreatedAt()),
                toInstant(user.getLastLoginAt()),
                toInstant(withdrawalRequestedAt),
                withdrawalRequestedAt == null
                        ? null
                        : toInstant(withdrawalRequestedAt.plusDays(withdrawalRetentionDays))
        );
    }

    private static Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC).toInstant();
    }
}
