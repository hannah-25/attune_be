package attune.journal.application;

import attune.common.error.BadRequestException;
import attune.common.error.notfound.JournalTagNotFoundException;
import attune.common.security.CustomUserDetails;
import attune.journal.application.dto.request.CheckJournalTagRequest;
import attune.journal.domain.model.JournalTag;
import attune.journal.domain.model.JournalTagCategory;
import attune.journal.domain.model.JournalTagLog;
import attune.journal.domain.model.JournalTagScope;
import attune.journal.domain.model.UserJournalTagPreference;
import attune.journal.domain.model.UserJournalTagPreferenceId;
import attune.journal.domain.repository.JournalTagLogRepository;
import attune.journal.domain.repository.JournalTagRepository;
import attune.journal.domain.repository.UserJournalTagPreferenceRepository;
import attune.user.application.UserZoneResolver;
import attune.user.domain.model.UserSetting;
import attune.user.domain.model.UserStatus;
import attune.user.domain.model.UserType;
import attune.user.domain.repository.UserSettingRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JournalTagCheckServiceTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Mock
    private JournalTagRepository journalTagRepository;
    @Mock
    private JournalTagLogRepository logRepository;
    @Mock
    private UserJournalTagPreferenceRepository preferenceRepository;
    @Mock
    private JournalTagLogSaver logSaver;
    @Mock
    private UserSettingRepository userSettingRepository;

    private final Clock clock = Clock.systemUTC();

    private JournalTagCheckService checkService;

    @BeforeEach
    void setUp() {
        checkService = new JournalTagCheckService(
                journalTagRepository, logRepository, preferenceRepository,
                logSaver, new UserZoneResolver(userSettingRepository), clock);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void repeatedCheckReturnsExistingLogWithoutSavingAnother() {
        UUID userId = UUID.randomUUID();
        LocalDate journalDate = LocalDate.now(SEOUL).minusDays(1);
        JournalTag tag = systemTag(1L);
        JournalTagLog existingLog = log(10L, userId, tag.getId(), journalDate);
        authenticate(userId);
        when(journalTagRepository.findById(tag.getId())).thenReturn(Optional.of(tag));
        when(preferenceRepository.findById(new UserJournalTagPreferenceId(userId, tag.getId())))
                .thenReturn(Optional.empty());
        when(logRepository.findByUserIdAndJournalTagIdAndJournalDate(
                userId, tag.getId(), journalDate))
                .thenReturn(Optional.of(existingLog));

        var response = checkService.check(
                tag.getId(), new CheckJournalTagRequest(journalDate));

        assertThat(response.tagId()).isEqualTo(tag.getId());
        assertThat(response.journalDate()).isEqualTo(journalDate);
        assertThat(response.checkedAt()).isEqualTo(existingLog.getCheckedAt());
        verifyNoInteractions(logSaver);
    }

    @Test
    void disabledTagCannotBeChecked() {
        UUID userId = UUID.randomUUID();
        JournalTag tag = systemTag(1L);
        UserJournalTagPreference preference = UserJournalTagPreference.create(
                userId, tag.getId(), false, false, LocalDateTime.now(SEOUL));
        authenticate(userId);
        when(journalTagRepository.findById(tag.getId())).thenReturn(Optional.of(tag));
        when(preferenceRepository.findById(new UserJournalTagPreferenceId(userId, tag.getId())))
                .thenReturn(Optional.of(preference));

        assertThatThrownBy(() -> checkService.check(
                tag.getId(), new CheckJournalTagRequest(LocalDate.now(SEOUL))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("disabled");

        verifyNoInteractions(logRepository, logSaver);
    }

    @Test
    void anotherUsersTagCannotBeChecked() {
        UUID userId = UUID.randomUUID();
        JournalTag tag = userTag(1L, UUID.randomUUID());
        authenticate(userId);
        when(journalTagRepository.findById(tag.getId())).thenReturn(Optional.of(tag));

        assertThatThrownBy(() -> checkService.check(
                tag.getId(), new CheckJournalTagRequest(LocalDate.now(SEOUL))))
                .isInstanceOf(JournalTagNotFoundException.class);

        verifyNoInteractions(preferenceRepository, logRepository, logSaver);
    }

    @Test
    void futureDateCannotBeUnchecked() {
        UUID userId = UUID.randomUUID();
        authenticate(userId);

        assertThatThrownBy(() ->
                checkService.uncheck(1L, LocalDate.now(SEOUL).plusDays(1)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("future");

        verifyNoInteractions(journalTagRepository, logRepository);
    }

    @Test
    void futureDateBoundaryFollowsUserTimezoneNotSeoul() {
        // 2026-07-04T02:00:00Z 시점: 서울은 07-04 11:00, 뉴욕은 아직 07-03 22:00.
        // 뉴욕 사용자에게 07-04는 미래이므로 서울 기준이 아니라 뉴욕 기준으로 거부돼야 한다.
        Clock fixed = Clock.fixed(Instant.parse("2026-07-04T02:00:00Z"), ZoneOffset.UTC);
        JournalTagCheckService nyService = new JournalTagCheckService(
                journalTagRepository, logRepository, preferenceRepository,
                logSaver, new UserZoneResolver(userSettingRepository), fixed);
        UUID userId = UUID.randomUUID();
        authenticate(userId);
        when(userSettingRepository.findById(userId))
                .thenReturn(Optional.of(settingWithZone(userId, "America/New_York")));

        assertThatThrownBy(() ->
                nyService.uncheck(1L, LocalDate.of(2026, 7, 4)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("future");

        verifyNoInteractions(journalTagRepository, logRepository);
    }

    @Test
    void accessiblePastCheckCanBeRemovedIdempotently() {
        UUID userId = UUID.randomUUID();
        LocalDate journalDate = LocalDate.now(SEOUL).minusDays(1);
        JournalTag tag = systemTag(1L);
        authenticate(userId);
        when(journalTagRepository.findById(tag.getId())).thenReturn(Optional.of(tag));

        checkService.uncheck(tag.getId(), journalDate);

        verify(logRepository).deleteByUserIdAndJournalTagIdAndJournalDate(
                userId, tag.getId(), journalDate);
        verify(preferenceRepository, never()).findById(org.mockito.ArgumentMatchers.any());
    }

    private void authenticate(UUID userId) {
        CustomUserDetails principal =
                CustomUserDetails.fromJwt(userId, UserType.USER, UserStatus.ACTIVE);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities())
        );
    }

    private UserSetting settingWithZone(UUID userId, String timezone) {
        return UserSetting.builder()
                .id(userId)
                .timezone(timezone)
                .build();
    }

    private JournalTag systemTag(Long id) {
        LocalDateTime now = LocalDateTime.now(SEOUL);
        return JournalTag.builder()
                .id(id)
                .category(JournalTagCategory.CONDITION)
                .name("평온")
                .tagType("CALM")
                .scope(JournalTagScope.SYSTEM)
                .ownerKey(JournalTag.SYSTEM_OWNER_KEY)
                .isActive(true)
                .defaultVisible(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private JournalTag userTag(Long id, UUID ownerUserId) {
        LocalDateTime now = LocalDateTime.now(SEOUL);
        return JournalTag.builder()
                .id(id)
                .category(JournalTagCategory.CONDITION)
                .name("사용자 태그")
                .tagType("CALM")
                .scope(JournalTagScope.USER)
                .ownerUserId(ownerUserId)
                .ownerKey(ownerUserId)
                .isActive(true)
                .defaultVisible(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private JournalTagLog log(
            Long id, UUID userId, Long tagId, LocalDate journalDate
    ) {
        return JournalTagLog.builder()
                .id(id)
                .userId(userId)
                .journalTagId(tagId)
                .journalDate(journalDate)
                .checkedAt(LocalDateTime.now(SEOUL).minusMinutes(1))
                .build();
    }
}
