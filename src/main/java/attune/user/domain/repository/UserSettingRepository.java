package attune.user.domain.repository;

import attune.user.domain.model.UserSetting;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface UserSettingRepository extends JpaRepository<UserSetting, UUID> {

    @Query("""
            SELECT us FROM UserSetting us
            JOIN us.user u
            WHERE us.reportNotification = true
              AND u.userStatus = attune.user.domain.model.UserStatus.ACTIVE
            """)
    List<UserSetting> findAllByReportNotificationTrue(Pageable pageable);

    @Query("""
            SELECT us FROM UserSetting us
            JOIN us.user u
            WHERE us.marketingNotification = true
              AND u.userStatus = attune.user.domain.model.UserStatus.ACTIVE
            """)
    List<UserSetting> findAllByMarketingNotificationTrue(Pageable pageable);

    @Query("""
            SELECT COUNT(us) FROM UserSetting us
            JOIN us.user u
            WHERE us.marketingNotification = true
              AND u.userStatus = attune.user.domain.model.UserStatus.ACTIVE
            """)
    long countMarketingTargets();
}