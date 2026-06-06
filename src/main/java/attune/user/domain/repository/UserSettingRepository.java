package attune.user.domain.repository;

import attune.user.domain.model.UserSetting;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserSettingRepository extends JpaRepository<UserSetting, UUID> {

    List<UserSetting> findAllByReportNotificationTrue(Pageable pageable);

    List<UserSetting> findAllByMarketingNotificationTrue(Pageable pageable);
}