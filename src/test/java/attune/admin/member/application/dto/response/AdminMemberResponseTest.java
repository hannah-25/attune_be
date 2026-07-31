package attune.admin.member.application.dto.response;

import attune.user.domain.model.OAuthProvider;
import attune.user.domain.model.User;
import attune.user.domain.model.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AdminMemberResponseTest {

    @Test
    void includesFullMemberContractAndConfiguredWithdrawalSchedule() {
        LocalDateTime withdrawalRequestedAt = LocalDateTime.of(2026, 6, 19, 10, 30);
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("member@attune.test")
                .nickname("회원")
                .userStatus(UserStatus.WITHDRAWAL)
                .provider(OAuthProvider.KAKAO)
                .createdAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .lastLoginAt(LocalDateTime.of(2026, 6, 1, 0, 0))
                .withdrawalAt(withdrawalRequestedAt)
                .build();

        AdminMemberResponse response = AdminMemberResponse.from(user, 45);

        assertThat(response.id()).isEqualTo(user.getId());
        assertThat(response.nickname()).isEqualTo("회원");
        assertThat(response.createdAt()).isEqualTo(Instant.parse("2024-12-31T15:00:00Z"));
        assertThat(response.lastLoginAt()).isEqualTo(Instant.parse("2026-05-31T15:00:00Z"));
        assertThat(response.withdrawalRequestedAt()).isEqualTo(Instant.parse("2026-06-19T01:30:00Z"));
        assertThat(response.withdrawalScheduledAt()).isEqualTo(Instant.parse("2026-08-03T01:30:00Z"));
    }
}
