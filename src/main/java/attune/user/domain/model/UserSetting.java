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

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    private boolean medicationNotification;
    private boolean reportNotification;
    private boolean marketingNotification;
    private boolean takeMedicationOnHoliday;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Theme theme;

    public void update(Boolean medicationNotification, Boolean reportNotification, Boolean marketingNotification, Boolean takeMedicationOnHoliday, Theme theme) {
        if (medicationNotification != null) this.medicationNotification = medicationNotification;
        if (reportNotification != null) this.reportNotification = reportNotification;
        if (marketingNotification != null) this.marketingNotification = marketingNotification;
        if (takeMedicationOnHoliday != null) this.takeMedicationOnHoliday = takeMedicationOnHoliday;
        if (theme != null) this.theme = theme;
    }

    public static UserSetting createDefault(User user) {
        return UserSetting.builder()
                .user(user)
                .medicationNotification(true)
                .reportNotification(true)
                .marketingNotification(false)
                .takeMedicationOnHoliday(false)
                .theme(Theme.SYSTEM)
                .build();
    }
}