package attune.onboarding.application;

import attune.common.error.badrequest.InvalidOnboardingRequestException;
import attune.journal.application.JournalTagCatalogService;
import attune.journal.domain.model.DailyGoal;
import attune.journal.domain.model.DailyGoalType;
import attune.journal.domain.repository.DailyGoalRepository;
import attune.journal.domain.repository.OnboardingGoalSnapshotRepository;
import attune.onboarding.application.dto.request.GoalRequest;
import attune.onboarding.application.dto.request.SymptomRequest;
import attune.onboarding.application.dto.response.GoalResponse;
import attune.onboarding.domain.model.OnboardingSymptom;
import attune.onboarding.domain.repository.AsrsAssessmentRepository;
import attune.onboarding.domain.repository.OnboardingSymptomRepository;
import attune.user.domain.model.User;
import attune.user.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AsrsAssessmentRepository asrsAssessmentRepository;
    @Mock
    private OnboardingSymptomRepository onboardingSymptomRepository;
    @Mock
    private DailyGoalRepository dailyGoalRepository;
    @Mock
    private OnboardingGoalSnapshotRepository onboardingGoalSnapshotRepository;
    @Mock
    private OnboardingAiService onboardingAiService;
    @Mock
    private JournalTagCatalogService journalTagCatalogService;

    @InjectMocks
    private OnboardingService onboardingService;

    @Test
    void saveSymptomFiltersNullAndBlankQuickOnboardingSelectionsBeforeSaving() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).build();
        SymptomRequest request = new SymptomRequest(
                null,
                null,
                Arrays.asList(null, " INATTENTION ", "", "  ", "TIME_MANAGEMENT"),
                Arrays.asList(null, DailyGoalType.WORK_STUDY, DailyGoalType.TIME_MANAGEMENT),
                true
        );
        ArgumentCaptor<OnboardingSymptom> symptomCaptor = ArgumentCaptor.forClass(OnboardingSymptom.class);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(onboardingSymptomRepository.save(symptomCaptor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        onboardingService.saveSymptom(userId, request);

        OnboardingSymptom savedSymptom = symptomCaptor.getValue();
        assertThat(savedSymptom.getSelectedSymptomTypes()).isEqualTo("INATTENTION,TIME_MANAGEMENT");
        assertThat(savedSymptom.getSelectedFunctionalAreas()).isEqualTo("WORK_STUDY,TIME_MANAGEMENT");
    }

    @Test
    void saveSymptomRejectsQuickOnboardingWhenNoValidSymptomTypesRemain() {
        UUID userId = UUID.randomUUID();
        SymptomRequest request = new SymptomRequest(
                null,
                null,
                Arrays.asList(null, "", "  "),
                List.of(DailyGoalType.WORK_STUDY),
                true
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(User.builder().id(userId).build()));

        assertThatThrownBy(() -> onboardingService.saveSymptom(userId, request))
                .isInstanceOf(InvalidOnboardingRequestException.class);

        verifyNoInteractions(onboardingSymptomRepository);
    }

    @Test
    void saveSymptomRejectsQuickOnboardingWhenNoValidFunctionalAreasRemain() {
        UUID userId = UUID.randomUUID();
        SymptomRequest request = new SymptomRequest(
                null,
                null,
                List.of("INATTENTION"),
                Arrays.asList(null, null),
                true
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(User.builder().id(userId).build()));

        assertThatThrownBy(() -> onboardingService.saveSymptom(userId, request))
                .isInstanceOf(InvalidOnboardingRequestException.class);

        verifyNoInteractions(onboardingSymptomRepository);
    }

    @Test
    void saveGoalsDeactivatesExistingGoalsAndReusesMatchingTitles() {
        UUID userId = UUID.randomUUID();
        DailyGoal matchingGoal = DailyGoal.builder()
                .id(1L)
                .userId(userId)
                .dailyGoal("same title")
                .type(DailyGoalType.WORK_STUDY)
                .isActive(true)
                .build();
        DailyGoal replacedGoal = DailyGoal.builder()
                .id(2L)
                .userId(userId)
                .dailyGoal("old title")
                .type(DailyGoalType.TIME_MANAGEMENT)
                .isActive(true)
                .build();
        GoalRequest request = new GoalRequest(
                List.of(
                        new GoalRequest.GoalItem("same title", DailyGoalType.LIFE_MANAGEMENT),
                        new GoalRequest.GoalItem("new title", DailyGoalType.EMOTIONAL_SOCIAL)
                ),
                null
        );

        when(dailyGoalRepository.findAllByUserId(userId)).thenReturn(List.of(matchingGoal, replacedGoal));
        when(dailyGoalRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        GoalResponse response = onboardingService.saveGoals(userId, request);

        assertThat(matchingGoal.isActive()).isTrue();
        assertThat(matchingGoal.getType()).isEqualTo(DailyGoalType.LIFE_MANAGEMENT);
        assertThat(replacedGoal.isActive()).isFalse();
        assertThat(response.goals()).hasSize(2);
        assertThat(response.goals().get(0).goalId()).isEqualTo(1L);
        assertThat(response.goals()).allMatch(GoalResponse.GoalItem::isActive);
        verify(dailyGoalRepository).saveAll(anyList());
    }

    @Test
    void saveGoalsUsesFirstExistingGoalWhenRepositoryReturnsDuplicateTitles() {
        UUID userId = UUID.randomUUID();
        DailyGoal firstGoal = DailyGoal.builder()
                .id(1L)
                .userId(userId)
                .dailyGoal("same title")
                .type(DailyGoalType.WORK_STUDY)
                .isActive(true)
                .build();
        DailyGoal duplicateGoal = DailyGoal.builder()
                .id(2L)
                .userId(userId)
                .dailyGoal("same title")
                .type(DailyGoalType.TIME_MANAGEMENT)
                .isActive(true)
                .build();
        GoalRequest request = new GoalRequest(
                List.of(new GoalRequest.GoalItem("same title", DailyGoalType.LIFE_MANAGEMENT)),
                null
        );

        when(dailyGoalRepository.findAllByUserId(userId)).thenReturn(List.of(firstGoal, duplicateGoal));
        when(dailyGoalRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        GoalResponse response = onboardingService.saveGoals(userId, request);

        assertThat(response.goals()).singleElement()
                .satisfies(goal -> assertThat(goal.goalId()).isEqualTo(1L));
        assertThat(firstGoal.isActive()).isTrue();
        assertThat(firstGoal.getType()).isEqualTo(DailyGoalType.LIFE_MANAGEMENT);
        assertThat(duplicateGoal.isActive()).isFalse();
        verify(dailyGoalRepository).saveAll(List.of(firstGoal));
    }

    @Test
    void saveGoalsRejectsDuplicateTitlesBeforeSaving() {
        UUID userId = UUID.randomUUID();
        GoalRequest request = new GoalRequest(
                List.of(
                        new GoalRequest.GoalItem("same title", DailyGoalType.WORK_STUDY),
                        new GoalRequest.GoalItem("same title", DailyGoalType.TIME_MANAGEMENT)
                ),
                null
        );

        assertThatThrownBy(() -> onboardingService.saveGoals(userId, request))
                .isInstanceOf(InvalidOnboardingRequestException.class);

        verifyNoInteractions(dailyGoalRepository);
    }
}
