package attune.onboarding.application;

import attune.common.error.OnboardingNotCompleteException;
import attune.common.error.notfound.UserNotFoundException;
import attune.onboarding.application.dto.request.AsrsRequest;
import attune.onboarding.application.dto.request.GoalRequest;
import attune.onboarding.application.dto.request.SymptomRequest;
import attune.onboarding.application.dto.response.AsrsResponse;
import attune.onboarding.application.dto.response.CompleteOnboardingResponse;
import attune.onboarding.application.dto.response.GoalResponse;
import attune.onboarding.application.dto.response.SymptomResponse;
import attune.onboarding.domain.model.AsrsAnswer;
import attune.onboarding.domain.model.AsrsAssessment;
import attune.onboarding.domain.model.OnboardingSymptom;
import attune.onboarding.domain.model.TreatmentGoal;
import attune.onboarding.domain.repository.AsrsAssessmentRepository;
import attune.onboarding.domain.repository.OnboardingSymptomRepository;
import attune.onboarding.domain.repository.TreatmentGoalRepository;
import attune.user.domain.model.User;
import attune.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final UserRepository userRepository;
    private final AsrsAssessmentRepository asrsAssessmentRepository;
    private final OnboardingSymptomRepository onboardingSymptomRepository;
    private final TreatmentGoalRepository treatmentGoalRepository;

    @Transactional
    public AsrsResponse saveAsrs(UUID userId, AsrsRequest request) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        int partAScore = request.answers().stream()
                .filter(a -> a.questionId() >= 1 && a.questionId() <= 6)
                .mapToInt(AsrsRequest.AnswerItem::score)
                .sum();
        int totalScore = request.answers().stream()
                .mapToInt(AsrsRequest.AnswerItem::score)
                .sum();

        List<AsrsAnswer> answers = request.answers().stream()
                .map(a -> new AsrsAnswer(a.questionId(), a.score()))
                .toList();


        LocalDateTime now = LocalDateTime.now();
        AsrsAssessment assessment = AsrsAssessment.builder()
                .user(user)
                .partAScore(partAScore)
                .totalScore(totalScore)
                .completedAt(now)
                .answers(answers)
                .build();

        asrsAssessmentRepository.save(assessment);

        return new AsrsResponse(assessment.getId(), partAScore, totalScore, now);
    }

    @Transactional
    public SymptomResponse saveSymptom(UUID userId, SymptomRequest request) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        LocalDateTime now = LocalDateTime.now();
        OnboardingSymptom symptom = OnboardingSymptom.builder()
                .user(user)
                .description(request.description())
                .savedAt(now)
                .build();

        onboardingSymptomRepository.save(symptom);

        return new SymptomResponse(symptom.getId(), now);
    }

    @Transactional
    public GoalResponse saveGoals(UUID userId, GoalRequest request) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        List<TreatmentGoal> goals = request.goals().stream()
                .map(item -> TreatmentGoal.builder()
                        .user(user)
                        .title(item.title())
                        .description(item.description())
                        .build())
                .toList();

        List<TreatmentGoal> saved = treatmentGoalRepository.saveAll(goals);

        List<GoalResponse.GoalItem> items = saved.stream()
                .map(g -> new GoalResponse.GoalItem(g.getId(), g.getTitle(), g.isActive()))
                .toList();

        return new GoalResponse(items);
    }

    @Transactional
    public CompleteOnboardingResponse completeOnboarding(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        boolean hasAsrs = asrsAssessmentRepository.existsByUser(user);
        boolean hasSymptom = onboardingSymptomRepository.existsByUser(user);
        boolean hasGoals = treatmentGoalRepository.existsByUser(user);

        if (!hasAsrs || !hasSymptom || !hasGoals) {
            throw new OnboardingNotCompleteException();
        }

        LocalDateTime now = LocalDateTime.now();
        user.completeOnboarding(now);

        return new CompleteOnboardingResponse(true, now);
    }
}
