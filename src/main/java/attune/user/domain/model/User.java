package attune.user.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String email;

    private String password;

    private String nickname;

    @Enumerated(EnumType.STRING)
    private UserType userType;

    @Enumerated(EnumType.STRING)
    private UserStatus userStatus;

    private String provider;
    private String providerId;

    private String profileImageUrl;

    private LocalDateTime onboardedAt;
    private boolean onboardingSkipped;

    private LocalDateTime withdrawalAt;

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void activate() {
        this.userStatus = UserStatus.ACTIVE;
    }

    public boolean isOnboarded() {
        return onboardedAt != null;
    }

    public void completeOnboarding(LocalDateTime completedAt) {
        this.onboardedAt = completedAt;
    }

    public void skipOnboarding() {
        this.onboardingSkipped = true;
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void changeProfileImageUrl(String url) {
        this.profileImageUrl = url;
    }

    public void withdraw() {
        this.userStatus = UserStatus.WITHDRAWAL;
        this.withdrawalAt = LocalDateTime.now();
    }

    public void restore() {
        this.userStatus = UserStatus.ACTIVE;
        this.withdrawalAt = null;
    }
}
