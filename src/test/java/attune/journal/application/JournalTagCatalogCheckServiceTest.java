package attune.journal.application;

import attune.common.error.BadRequestException;
import attune.common.security.CustomUserDetails;
import attune.journal.application.dto.response.CatalogTagCheckResponse;
import attune.journal.domain.model.ConditionLog;
import attune.journal.domain.model.ConditionTag;
import attune.journal.domain.model.JournalTag;
import attune.journal.domain.model.JournalTagCategory;
import attune.journal.domain.model.JournalTagScope;
import attune.journal.domain.model.UserJournalTagPreference;
import attune.journal.domain.repository.ConditionLogRepository;
import attune.journal.domain.repository.ConditionTagRepository;
import attune.journal.domain.repository.JournalTagRepository;
import attune.journal.domain.repository.LegacyJournalTagMappingRepository;
import attune.journal.domain.repository.SideEffectLogRepository;
import attune.journal.domain.repository.SideEffectTagRepository;
import attune.journal.domain.repository.TroubleLogRepository;
import attune.journal.domain.repository.TroubleTagRepository;
import attune.journal.domain.repository.UserJournalTagPreferenceRepository;
import attune.user.domain.model.UserStatus;
import attune.user.domain.model.UserType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JournalTagCatalogCheckServiceTest {

    @Mock
    private JournalTagRepository journalTagRepository;
    @Mock
    private LegacyJournalTagMappingRepository mappingRepository;
    @Mock
    private UserJournalTagPreferenceRepository preferenceRepository;
    @Mock
    private ConditionTagRepository conditionTagRepository;
    @Mock
    private SideEffectTagRepository sideEffectTagRepository;
    @Mock
    private TroubleTagRepository troubleTagRepository;
    @Mock
    private ConditionLogRepository conditionLogRepository;
    @Mock
    private SideEffectLogRepository sideEffectLogRepository;
    @Mock
    private TroubleLogRepository troubleLogRepository;
    @InjectMocks
    private JournalTagCatalogCheckService checkService;

    private UUID userId;

    @BeforeEach
    void authenticate() {
        userId = UUID.randomUUID();
        CustomUserDetails principal = CustomUserDetails.fromJwt(userId, UserType.USER, UserStatus.ACTIVE);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void firstCheckOfUnmappedSystemTagCreatesCompatibilityTagAndDualWriteLog() {
        JournalTag systemTag = JournalTag.builder()
                .id(10L)
                .category(JournalTagCategory.CONDITION)
                .name("calm")
                .tagType("CALM")
                .scope(JournalTagScope.SYSTEM)
                .ownerKey(JournalTag.SYSTEM_OWNER_KEY)
                .isActive(true)
                .defaultVisible(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        ArgumentCaptor<ConditionLog> logCaptor = ArgumentCaptor.forClass(ConditionLog.class);
        ArgumentCaptor<UserJournalTagPreference> preferenceCaptor =
                ArgumentCaptor.forClass(UserJournalTagPreference.class);

when(journalTagRepository.findById(10L)).thenReturn(Optional.of(systemTag));
        when(preferenceRepository.findById(any())).thenReturn(Optional.empty());
        when(mappingRepository.findAllByUserIdAndLegacyCategory(userId, JournalTagCategory.CONDITION)).thenReturn(List.of());
        when(conditionTagRepository.save(any(ConditionTag.class))).thenAnswer(invocation -> {
            ConditionTag source = invocation.getArgument(0);
            return ConditionTag.builder()
                    .id(42L)
                    .userId(source.getUserId())
                    .condition(source.getCondition())
                    .conditionType(source.getConditionType())
                    .isActive(source.isActive())
                    .visible(source.isVisible())
                    .build();
        });

        CatalogTagCheckResponse response = checkService.check(10L);

        verify(conditionLogRepository).save(logCaptor.capture());
        assertThat(response.catalogTagId()).isEqualTo(10L);
        assertThat(logCaptor.getValue().getConditionTagId()).isEqualTo(42L);
        assertThat(logCaptor.getValue().getJournalTagId()).isEqualTo(10L);
        assertThat(logCaptor.getValue().getUserId()).isEqualTo(userId);
        verify(mappingRepository).save(any());
        verify(preferenceRepository).save(preferenceCaptor.capture());
        assertThat(preferenceCaptor.getValue().getUserId()).isEqualTo(userId);
        assertThat(preferenceCaptor.getValue().getJournalTagId()).isEqualTo(10L);
        assertThat(preferenceCaptor.getValue().isEnabled()).isTrue();
        assertThat(preferenceCaptor.getValue().isVisible()).isTrue();
    }

    @Test
    void disabledTagCheckReturnsBadRequest() {
        JournalTag systemTag = JournalTag.builder()
                .id(10L)
                .category(JournalTagCategory.CONDITION)
                .name("calm")
                .tagType("CALM")
                .scope(JournalTagScope.SYSTEM)
                .ownerKey(JournalTag.SYSTEM_OWNER_KEY)
                .isActive(true)
                .defaultVisible(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

when(journalTagRepository.findById(10L)).thenReturn(Optional.of(systemTag));
        when(preferenceRepository.findById(any())).thenReturn(Optional.of(
                attune.journal.domain.model.UserJournalTagPreference.create(userId, 10L, false, false)));

        assertThatThrownBy(() -> checkService.check(10L))
                .isInstanceOf(BadRequestException.class);
    }
}
