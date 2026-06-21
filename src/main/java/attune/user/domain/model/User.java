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

    @Enumerated(EnumType.STRING)
    private OAuthProvider provider;
    private String providerId;

    private String profileImageUrl;

    private LocalDateTime onboardedAt;
    private boolean onboardingSkipped;

    private LocalDateTime withdrawalAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastLoginAt;

    @PrePersist
    void initializeCreatedAt() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

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
        this.onboardingSkipped = false;
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

    public void withdraw(LocalDateTime now) {
        this.userStatus = UserStatus.WITHDRAWAL;
        this.withdrawalAt = now;
    }

    public void restore() {
        this.userStatus = UserStatus.ACTIVE;
        this.withdrawalAt = null;
    }

    public void suspend() {
        this.userStatus = UserStatus.SUSPENDED;
    }

    public void softDelete() {
        this.userStatus = UserStatus.DELETED;
        this.email = "deleted_" + UUID.randomUUID() + "@deleted.attune.me";
        this.nickname = "deleted_" + this.id.toString().replace("-", "");
        this.password = null;
        this.provider = null;
        this.providerId = null;
        this.profileImageUrl = null;
    }

    public void recordLogin(LocalDateTime loggedInAt) {
        this.lastLoginAt = loggedInAt;
    }

    public void linkSocialProvider(OAuthProvider provider, String providerId) {
        this.provider = provider;
        this.providerId = providerId;
    }
}
