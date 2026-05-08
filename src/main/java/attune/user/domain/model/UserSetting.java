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

    private boolean alarmEnabled;
    private boolean takeMedicationOnHoliday;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Theme theme;

    public void update(Boolean alarmEnabled, Boolean takeMedicationOnHoliday, Theme theme) {
        if (alarmEnabled != null) this.alarmEnabled = alarmEnabled;
        if (takeMedicationOnHoliday != null) this.takeMedicationOnHoliday = takeMedicationOnHoliday;
        if (theme != null) this.theme = theme;
    }

    public static UserSetting createDefault(User user) {
        return UserSetting.builder()
                .user(user)
                .alarmEnabled(true)
                .takeMedicationOnHoliday(false)
                .theme(Theme.SYSTEM)
                .build();
    }
}