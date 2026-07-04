package attune.user.application;

import attune.common.error.BadRequestException;
import attune.user.application.dto.request.UpdateUserSettingRequest;
import attune.user.application.dto.response.UserSettingResponse;
import attune.user.domain.model.Theme;
import attune.user.domain.model.UserSetting;
import attune.user.domain.repository.UserRepository;
import attune.user.domain.repository.UserSettingRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserSettingServiceTest {

    private final UserSettingRepository userSettingRepository = mock(UserSettingRepository.class);
    private final UserSettingService service = new UserSettingService(
            userSettingRepository,
            mock(UserRepository.class)
    );

    @Test
    void updateSettingsStoresValidTimezone() {
        UUID userId = UUID.randomUUID();
        UserSetting setting = UserSetting.builder()
                .id(userId)
                .medicationNotification(true)
                .reportNotification(true)
                .marketingNotification(false)
                .communityNotification(true)
                .todoNotification(true)
                .takeMedicationOnHoliday(false)
                .theme(Theme.SYSTEM)
                .timezone(UserSetting.DEFAULT_TIMEZONE)
                .build();
        when(userSettingRepository.findById(userId)).thenReturn(Optional.of(setting));

        UserSettingResponse response = service.updateSettings(
                userId,
                new UpdateUserSettingRequest(null, null, null, null, null, null, null, "America/New_York")
        );

        assertThat(setting.getTimezone()).isEqualTo("America/New_York");
        assertThat(response.timezone()).isEqualTo("America/New_York");
    }

    @Test
    void updateSettingsRejectsInvalidTimezone() {
        UUID userId = UUID.randomUUID();
        UserSetting setting = UserSetting.builder()
                .id(userId)
                .theme(Theme.SYSTEM)
                .timezone(UserSetting.DEFAULT_TIMEZONE)
                .build();
        when(userSettingRepository.findById(userId)).thenReturn(Optional.of(setting));

        assertThatThrownBy(() -> service.updateSettings(
                userId,
                new UpdateUserSettingRequest(null, null, null, null, null, null, null, "Not/A_Timezone")
        )).isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateSettingsRejectsBlankTimezone() {
        UUID userId = UUID.randomUUID();
        UserSetting setting = UserSetting.builder()
                .id(userId)
                .theme(Theme.SYSTEM)
                .timezone(UserSetting.DEFAULT_TIMEZONE)
                .build();
        when(userSettingRepository.findById(userId)).thenReturn(Optional.of(setting));

        assertThatThrownBy(() -> service.updateSettings(
                userId,
                new UpdateUserSettingRequest(null, null, null, null, null, null, null, " ")
        )).isInstanceOf(BadRequestException.class);
    }
}
