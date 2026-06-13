package attune.onboarding.application;

import attune.common.error.badrequest.InvalidOnboardingRequestException;
import attune.journal.domain.model.DailyGoal;
import attune.journal.domain.model.DailyGoalType;
import attune.journal.domain.repository.DailyGoalRepository;
import attune.journal.domain.repository.TroubleTagRepository;
import attune.onboarding.application.dto.request.GoalRequest;
import attune.onboarding.application.dto.response.GoalResponse;
import attune.onboarding.domain.repository.AsrsAssessmentRepository;
import attune.onboarding.domain.repository.OnboardingSymptomRepository;
import attune.user.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
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
    private TroubleTagRepository troubleTagRepository;
    @Mock
    private OnboardingAiService onboardingAiService;

    @InjectMocks
    private OnboardingService onboardingService;

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
