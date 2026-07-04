package attune.user.application;

import attune.user.domain.model.UserSetting;
import attune.user.domain.repository.UserSettingRepository;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserZoneResolverTest {

    private final UserSettingRepository userSettingRepository = mock(UserSettingRepository.class);
    private final UserZoneResolver resolver = new UserZoneResolver(userSettingRepository);

    @Test
    void resolvesStoredTimezone() {
        UUID userId = UUID.randomUUID();
        when(userSettingRepository.findById(userId))
                .thenReturn(Optional.of(UserSetting.builder().id(userId).timezone("America/New_York").build()));

        assertThat(resolver.resolve(userId)).isEqualTo(ZoneId.of("America/New_York"));
    }

    @Test
    void fallsBackToDefaultWhenSettingMissing() {
        UUID userId = UUID.randomUUID();
        when(userSettingRepository.findById(userId)).thenReturn(Optional.empty());

        assertThat(resolver.resolve(userId)).isEqualTo(ZoneId.of(UserSetting.DEFAULT_TIMEZONE));
    }

    @Test
    void fallsBackToDefaultWhenTimezoneInvalid() {
        UUID userId = UUID.randomUUID();
        when(userSettingRepository.findById(userId))
                .thenReturn(Optional.of(UserSetting.builder().id(userId).timezone("Not/AZone").build()));

        assertThat(resolver.resolve(userId)).isEqualTo(ZoneId.of(UserSetting.DEFAULT_TIMEZONE));
    }
}
