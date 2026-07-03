package attune.user.application;

import attune.user.domain.model.UserSetting;
import attune.user.domain.repository.UserSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/**
 * 사용자 최신 timezone(user_settings.timezone)을 해석하는 공용 헬퍼.
 * 저장값이 없거나 유효하지 않으면 {@link UserSetting#DEFAULT_TIMEZONE}으로 폴백한다.
 */
@Component
@RequiredArgsConstructor
public class UserZoneResolver {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of(UserSetting.DEFAULT_TIMEZONE);

    private final UserSettingRepository userSettingRepository;

    public ZoneId resolve(UUID userId) {
        return userSettingRepository.findById(userId)
                .map(UserSetting::getTimezone)
                .map(UserZoneResolver::parseOrDefault)
                .orElse(DEFAULT_ZONE);
    }

    /** 사용자 timezone 기준 오늘 날짜. */
    public LocalDate today(UUID userId) {
        return LocalDate.now(resolve(userId));
    }

    private static ZoneId parseOrDefault(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException e) {
            return DEFAULT_ZONE;
        }
    }
}
