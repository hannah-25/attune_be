package attune.admin.member.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AdminMemberWithdrawalPolicy {

    private final int retentionDays;

    public AdminMemberWithdrawalPolicy(
            @Value("${app.user.deletion-retention-days:30}") int retentionDays
    ) {
        this.retentionDays = retentionDays;
    }

    public int retentionDays() {
        return retentionDays;
    }
}
