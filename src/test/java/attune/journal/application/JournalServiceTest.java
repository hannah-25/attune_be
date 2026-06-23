package attune.journal.application;

import attune.common.security.CustomUserDetails;
import attune.journal.application.dto.response.JournalTagResponse;
import attune.journal.domain.model.JournalTag;
import attune.journal.domain.model.JournalTagCategory;
import attune.journal.domain.model.JournalTagLog;
import attune.journal.domain.model.JournalTagScope;
import attune.journal.domain.repository.DailyGoalLogRepository;
import attune.journal.domain.repository.DailyGoalRepository;
import attune.journal.domain.repository.DailyStatusLogRepository;
import attune.journal.domain.repository.JournalTagLogRepository;
import attune.journal.domain.repository.JournalTagLogView;
import attune.journal.domain.repository.MemoRepository;
import attune.user.domain.model.UserStatus;
import attune.user.domain.model.UserType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JournalServiceTest {

    @Mock
    private JournalTagLogRepository journalTagLogRepository;
    @Mock
    private JournalTagService journalTagService;
    @Mock
    private DailyStatusLogRepository dailyStatusLogRepository;
    @Mock
    private DailyGoalRepository dailyGoalRepository;
    @Mock
    private DailyGoalLogRepository dailyGoalLogRepository;
    @Mock
    private MemoRepository memoRepository;

    @InjectMocks
    private JournalService journalService;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void detailSeparatesCurrentActiveTagsFromHistoricalChecks() {
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 6, 20);
        authenticate(userId);
        stubActiveTags();

        JournalTagLogView inactiveCondition = logView(
                50L, JournalTagCategory.CONDITION, "과거 상태", "CALM", false, date);
        JournalTagLogView inactiveTrouble = logView(
                51L, JournalTagCategory.TROUBLE, "과거 어려움", "INATTENTION", false, date);
        when(journalTagLogRepository.findAllWithTagByUserIdAndJournalDate(userId, date))
                .thenReturn(List.of(inactiveCondition, inactiveTrouble));
        when(dailyStatusLogRepository.findByUserIdAndDate(userId, date))
                .thenReturn(Optional.empty());
        when(dailyGoalLogRepository.findAllInRangeWithGoal(userId, date, date))
                .thenReturn(List.of());
        when(memoRepository.findByUserIdAndJournalDate(userId, date))
                .thenReturn(Optional.empty());

        var response = journalService.getJournal(date);

        assertThat(response.activeTags().conditions())
                .extracting(tag -> tag.tagId())
                .containsExactly(1L);
        assertThat(response.activeTags().troubles())
                .extracting(tag -> tag.tagId())
                .containsExactly(3L);
        assertThat(response.checked().conditions())
                .extracting(check -> check.tagId())
                .containsExactly(50L);
        assertThat(response.checked().troubles())
                .extracting(check -> check.tagId())
                .containsExactly(51L);
    }

    @Test
    void bulkGroupsHistoricalChecksByDateAndCategory() {
        UUID userId = UUID.randomUUID();
        LocalDate start = LocalDate.of(2026, 6, 20);
        LocalDate end = start.plusDays(1);
        authenticate(userId);
        stubActiveTags();

        when(journalTagLogRepository.findAllWithTagByUserIdAndJournalDateBetween(
                userId, start, end))
                .thenReturn(List.of(
                        logView(50L, JournalTagCategory.CONDITION,
                                "과거 상태", "CALM", false, start),
                        logView(51L, JournalTagCategory.SIDE_EFFECT,
                                "과거 부작용", "NONE", false, end),
                        logView(52L, JournalTagCategory.TROUBLE,
                                "과거 어려움", "INATTENTION", false, end)
                ));
        when(dailyStatusLogRepository.findByUserIdAndDateBetween(userId, start, end))
                .thenReturn(List.of());
        when(dailyGoalLogRepository.findAllInRangeWithGoal(userId, start, end))
                .thenReturn(List.of());
        when(memoRepository.findByUserIdAndJournalDateBetween(userId, start, end))
                .thenReturn(List.of());

        var response = journalService.getJournalsBulk(start, end);

        assertThat(response.journals()).hasSize(2);
        assertThat(response.journals().get(0).date()).isEqualTo(start);
        assertThat(response.journals().get(0).checked().conditions())
                .extracting(check -> check.tagId())
                .containsExactly(50L);
        assertThat(response.journals().get(0).checked().sideEffects()).isEmpty();
        assertThat(response.journals().get(0).checked().troubles()).isEmpty();

        assertThat(response.journals().get(1).date()).isEqualTo(end);
        assertThat(response.journals().get(1).checked().conditions()).isEmpty();
        assertThat(response.journals().get(1).checked().sideEffects())
                .extracting(check -> check.tagId())
                .containsExactly(51L);
        assertThat(response.journals().get(1).checked().troubles())
                .extracting(check -> check.tagId())
                .containsExactly(52L);
        verify(journalTagLogRepository)
                .findAllWithTagByUserIdAndJournalDateBetween(userId, start, end);
    }

    private void stubActiveTags() {
        when(journalTagService.getTags(JournalTagCategory.CONDITION, false))
                .thenReturn(List.of(responseTag(
                        1L, JournalTagCategory.CONDITION, "평온", "CALM")));
        when(journalTagService.getTags(JournalTagCategory.SIDE_EFFECT, false))
                .thenReturn(List.of(responseTag(
                        2L, JournalTagCategory.SIDE_EFFECT, "두통", "NONE")));
        when(journalTagService.getTags(JournalTagCategory.TROUBLE, false))
                .thenReturn(List.of(responseTag(
                        3L, JournalTagCategory.TROUBLE, "깜빡함", "INATTENTION")));
        when(dailyGoalRepository.findAllByUserIdAndIsActiveTrue(
                org.mockito.ArgumentMatchers.any(UUID.class)))
                .thenReturn(List.of());
    }

    private JournalTagResponse responseTag(
            Long id, JournalTagCategory category, String name, String type
    ) {
        return new JournalTagResponse(
                id, category, name, type, JournalTagScope.SYSTEM, true, true);
    }

    private JournalTagLogView logView(
            Long id,
            JournalTagCategory category,
            String name,
            String type,
            boolean active,
            LocalDate date
    ) {
        LocalDateTime checkedAt = date.atTime(10, 0);
        JournalTag tag = JournalTag.builder()
                .id(id)
                .category(category)
                .name(name)
                .tagType(type)
                .scope(JournalTagScope.USER)
                .ownerUserId(UUID.randomUUID())
                .ownerKey(UUID.randomUUID())
                .isActive(active)
                .defaultVisible(false)
                .createdAt(checkedAt.minusDays(1))
                .updatedAt(checkedAt)
                .build();
        JournalTagLog log = JournalTagLog.builder()
                .id(id + 100L)
                .userId(UUID.randomUUID())
                .journalTagId(id)
                .journalDate(date)
                .checkedAt(checkedAt)
                .build();
        return new JournalTagLogView(log, tag);
    }

    private void authenticate(UUID userId) {
        CustomUserDetails principal =
                CustomUserDetails.fromJwt(userId, UserType.USER, UserStatus.ACTIVE);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities())
        );
    }
}
