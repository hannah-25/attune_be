package attune.user.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void softDeleteAnonymizesCredentialsAndSocialProfile() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("member@example.com")
                .password("encoded-password")
                .nickname("member")
                .userStatus(UserStatus.WITHDRAWAL)
                .provider(OAuthProvider.GOOGLE)
                .providerId("google-user-id")
                .profileImageUrl("https://example.com/profile.png")
                .build();

        user.softDelete();

        assertThat(user.getUserStatus()).isEqualTo(UserStatus.DELETED);
        assertThat(user.getEmail()).startsWith("deleted_").endsWith("@deleted.attune.me");
        assertThat(user.getNickname()).isEqualTo(
                "deleted_" + userId.toString().replace("-", "").substring(0, 12)
        );
        assertThat(user.getPassword()).isNull();
        assertThat(user.getProvider()).isNull();
        assertThat(user.getProviderId()).isNull();
        assertThat(user.getProfileImageUrl()).isNull();
    }
}
