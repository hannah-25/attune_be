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
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    private String theme;
    private String dateFormat;
    private String timeFormat;
    private String weekStartDay;
    private boolean breakTimeAlert;
    private boolean emailDeadlineAlert;
    private boolean emailSecurityAlert;
    private boolean emailMarketingAlert;
    private boolean pushBrowserAlert;
    private boolean pushTimerAlert;
    private boolean pushScheduleAlert;
    private boolean pushMedicationAlert;
    private boolean pushCommunityAlert;
    private boolean pushConsultationAlert;
    private boolean pushCalendarAlert;

    public void updateEmailNotification(boolean emailDeadlineAlert, boolean emailSecurityAlert, boolean emailMarketingAlert) {
        this.emailDeadlineAlert = emailDeadlineAlert;
        this.emailSecurityAlert = emailSecurityAlert;
        this.emailMarketingAlert = emailMarketingAlert;
    }

    public void updatePushNotification(boolean pushBrowserAlert, boolean pushTimerAlert, boolean pushScheduleAlert,
                                        boolean pushMedicationAlert, boolean pushCommunityAlert,
                                        boolean pushConsultationAlert, boolean pushCalendarAlert) {
        this.pushBrowserAlert = pushBrowserAlert;
        this.pushTimerAlert = pushTimerAlert;
        this.pushScheduleAlert = pushScheduleAlert;
        this.pushMedicationAlert = pushMedicationAlert;
        this.pushCommunityAlert = pushCommunityAlert;
        this.pushConsultationAlert = pushConsultationAlert;
        this.pushCalendarAlert = pushCalendarAlert;
    }

    public void updateTheme(String theme) {
        this.theme = theme;
    }

    public void updateLocalization( String dateFormat, String timeFormat, String weekStartDay) {
        this.dateFormat = dateFormat;
        this.timeFormat = timeFormat;
        this.weekStartDay = weekStartDay;
    }

    public void updateWorkspaceSettings(boolean breakTimeAlert) {
        this.breakTimeAlert = breakTimeAlert;
    }

    public static UserSetting createDefault(User user) {
        return UserSetting.builder()
                .user(user)
                .theme("system")
                .dateFormat("YYYY-MM-DD")
                .timeFormat("24h")
                .weekStartDay("monday")
                .breakTimeAlert(false)
                .emailDeadlineAlert(false)
                .emailSecurityAlert(false)
                .emailMarketingAlert(false)
                .pushBrowserAlert(false)
                .pushTimerAlert(false)
                .pushScheduleAlert(false)
                .pushMedicationAlert(false)
                .pushCommunityAlert(false)
                .pushConsultationAlert(false)
                .pushCalendarAlert(false)
                .build();
    }
}