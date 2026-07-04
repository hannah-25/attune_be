package attune.user.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "user_settings")
public class UserSetting {

    public static final String DEFAULT_TIMEZONE = "Asia/Seoul";

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    private boolean medicationNotification;
    private boolean reportNotification;
    private boolean marketingNotification;
    private boolean communityNotification;
    private boolean todoNotification;
    private boolean takeMedicationOnHoliday;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Theme theme;

    @Builder.Default
    @Column(nullable = false, length = 64)
    private String timezone = DEFAULT_TIMEZONE;

    public void update(Boolean medicationNotification, Boolean reportNotification, Boolean marketingNotification,
                       Boolean communityNotification, Boolean todoNotification,
                       Boolean takeMedicationOnHoliday, Theme theme, String timezone) {
        if (medicationNotification != null) this.medicationNotification = medicationNotification;
        if (reportNotification != null) this.reportNotification = reportNotification;
        if (marketingNotification != null) this.marketingNotification = marketingNotification;
        if (communityNotification != null) this.communityNotification = communityNotification;
        if (todoNotification != null) this.todoNotification = todoNotification;
        if (takeMedicationOnHoliday != null) this.takeMedicationOnHoliday = takeMedicationOnHoliday;
        if (theme != null) this.theme = theme;
        if (timezone != null) this.timezone = timezone;
    }

    public static UserSetting createDefault(User user) {
        return UserSetting.builder()
                .user(user)
                .medicationNotification(true)
                .reportNotification(true)
                .marketingNotification(false)
                .communityNotification(true)
                .todoNotification(true)
                .takeMedicationOnHoliday(false)
                .theme(Theme.SYSTEM)
                .timezone(DEFAULT_TIMEZONE)
                .build();
    }

    @PrePersist
    void initializeDefaults() {
        if (timezone == null) {
            timezone = DEFAULT_TIMEZONE;
        }
    }
}
