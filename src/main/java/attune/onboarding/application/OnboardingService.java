package attune.onboarding.application;

import attune.common.error.badrequest.InvalidOnboardingRequestException;
import attune.common.error.badrequest.OnboardingNotCompleteException;
import attune.common.error.notfound.AsrsAssessmentNotFoundException;
import attune.common.error.notfound.UserNotFoundException;
import attune.journal.domain.model.DailyGoal;
import attune.journal.domain.model.DailyGoalType;
import attune.journal.domain.model.TroubleTag;
import attune.journal.domain.repository.DailyGoalRepository;
import attune.journal.domain.repository.TroubleTagRepository;
import attune.onboarding.application.dto.request.AsrsRequest;
import attune.onboarding.application.dto.request.GoalRequest;
import attune.onboarding.application.dto.request.SymptomRequest;
import attune.onboarding.application.dto.response.AiRecommendationResponse;
import attune.onboarding.application.dto.response.AsrsResponse;
import attune.onboarding.application.dto.response.CompleteOnboardingResponse;
import attune.onboarding.application.dto.response.GoalResponse;
import attune.onboarding.application.dto.response.OnboardingHistoryDetailResponse;
import attune.onboarding.application.dto.response.OnboardingHistoryResponse;
import attune.onboarding.application.dto.response.OnboardingStatusResponse;
import attune.onboarding.application.dto.response.SymptomResponse;
import attune.onboarding.domain.model.AsrsAnswer;
import attune.onboarding.domain.model.AsrsAssessment;
import attune.onboarding.domain.model.OnboardingSymptom;
import attune.onboarding.domain.repository.AsrsAssessmentRepository;
import attune.onboarding.domain.repository.OnboardingSymptomRepository;
import attune.user.domain.model.User;
import attune.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final UserRepository userRepository;
    private final AsrsAssessmentRepository asrsAssessmentRepository;
    private final OnboardingSymptomRepository onboardingSymptomRepository;
    private final DailyGoalRepository dailyGoalRepository;
    private final TroubleTagRepository troubleTagRepository;
    private final OnboardingAiService onboardingAiService;

    @Transactional(readOnly = true)
    public OnboardingStatusResponse getOnboardingStatus(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        if (user.isOnboarded()) return OnboardingStatusResponse.completed(user);
        if (user.isOnboardingSkipped()) return OnboardingStatusResponse.ofSkipped();

        boolean hasSymptom = onboardingSymptomRepository.existsByUser(user);
        boolean hasAsrs = asrsAssessmentRepository.existsByUser(user);
        boolean hasGoals = dailyGoalRepository.existsByUserId(userId);

        int resumeStep;
        if (hasGoals)        resumeStep = 5;
        else if (hasAsrs)    resumeStep = 4;
        else if (hasSymptom) resumeStep = 3;
        else                 resumeStep = 2;

        return OnboardingStatusResponse.inProgress(resumeStep);
    }

    @Transactional
    public void skipOnboarding(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        user.skipOnboarding();
    }

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
        OnboardingSymptom.OnboardingSymptomBuilder builder = OnboardingSymptom.builder()
                .user(user)
                .savedAt(now)
                .isQuickOnboarding(request.isQuickOnboarding());

        if (request.isQuickOnboarding()) {
            // 경로 B
            List<String> selectedSymptomTypes = request.selectedSymptomTypes() == null
                    ? List.of()
                    : request.selectedSymptomTypes().stream()
                            .filter(Objects::nonNull)
                            .map(String::strip)
                            .filter(value -> !value.isBlank())
                            .toList();
            List<DailyGoalType> selectedFunctionalAreas = request.selectedFunctionalAreas() == null
                    ? List.of()
                    : request.selectedFunctionalAreas().stream()
                            .filter(Objects::nonNull)
                            .toList();

            if (selectedSymptomTypes.isEmpty()) {
                throw new InvalidOnboardingRequestException("selectedSymptomTypes는 필수입니다.");
            }
            if (selectedFunctionalAreas.isEmpty()) {
                throw new InvalidOnboardingRequestException("selectedFunctionalAreas는 필수입니다.");
            }
            builder.selectedSymptomTypes(String.join(",", selectedSymptomTypes))
                   .selectedFunctionalAreas(selectedFunctionalAreas.stream()
                           .map(Enum::name)
                           .collect(Collectors.joining(",")));
        } else {
            // 경로 A
            if (request.description() == null || request.description().isBlank()) {
                throw new InvalidOnboardingRequestException("description은 필수입니다.");
            }
            builder.description(request.description())
                   .emotionalEvent(request.emotionalEvent());
        }

        OnboardingSymptom symptom = onboardingSymptomRepository.save(builder.build());
        return new SymptomResponse(symptom.getId(), now);
    }

    /**
     * Gemini 분석 호출 — ASRS + 증상 서술(경로 A) 또는 취약 영역 선택(경로 B) 완료 후 호출.
     * 결과는 저장하지 않고 프론트에 반환, 사용자가 확인/편집 후 saveGoals()로 확정.
     */
    @Transactional(readOnly = true)
    public AiRecommendationResponse getAiRecommendations(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        OnboardingSymptom symptom = onboardingSymptomRepository
                .findTopByUserOrderBySavedAtDesc(user)
                .orElseThrow(() -> new InvalidOnboardingRequestException("증상 서술이 없습니다."));

        GeminiOnboardingResponse geminiResponse;

        if (symptom.isQuickOnboarding()) {
            // 경로 B
            if (symptom.getSelectedSymptomTypes() == null || symptom.getSelectedFunctionalAreas() == null) {
                throw new InvalidOnboardingRequestException("경로 B 증상 데이터가 올바르지 않습니다.");
            }
            List<String> symptomTypes = List.of(symptom.getSelectedSymptomTypes().split(","));
            List<DailyGoalType> functionalAreas = Arrays.stream(symptom.getSelectedFunctionalAreas().split(","))
                    .map(name -> DailyGoalType.valueOf(name.trim()))
                    .toList();
            geminiResponse = onboardingAiService.analyzeQuickOnboarding(symptomTypes, functionalAreas);
        } else {
            // 경로 A
            AsrsAssessment assessment = asrsAssessmentRepository
                    .findTopByUserOrderByCompletedAtDesc(user)
                    .orElseThrow(() -> new InvalidOnboardingRequestException("ASRS 결과가 없습니다."));

            int inattentionScore = assessment.getAnswers().stream()
                    .filter(a -> a.getQuestionId() >= 1 && a.getQuestionId() <= 9)
                    .mapToInt(AsrsAnswer::getScore)
                    .sum();
            int hyperactivityScore = assessment.getAnswers().stream()
                    .filter(a -> a.getQuestionId() >= 10 && a.getQuestionId() <= 18)
                    .mapToInt(AsrsAnswer::getScore)
                    .sum();

            geminiResponse = onboardingAiService.analyzeFullOnboarding(
                    symptom.getDescription(), inattentionScore, hyperactivityScore);
        }

        // 태그: 유저 태그 전체 + Gemini 추천 여부 표시
        List<TroubleTag> userTags = troubleTagRepository.findAllByUserIdAndIsActiveTrue(userId);
        Set<String> recommended = new HashSet<>(geminiResponse.visibleTags());

        List<AiRecommendationResponse.TagItem> tagItems = userTags.stream()
                .map(t -> new AiRecommendationResponse.TagItem(
                        t.getId(), t.getTrouble(), t.getType().name(),
                        recommended.contains(t.getTrouble())))
                .toList();

        // 목표: Korean functionalArea → DailyGoalType 매핑
        List<AiRecommendationResponse.GoalItem> goalItems = geminiResponse.treatmentGoals().stream()
                .map(g -> new AiRecommendationResponse.GoalItem(g.goal(), mapFunctionalArea(g.functionalArea())))
                .toList();

        return new AiRecommendationResponse(tagItems, goalItems);
    }

    @Transactional
    public GoalResponse saveGoals(UUID userId, GoalRequest request) {
        Set<String> requestedTitles = new HashSet<>();
        boolean hasDuplicateTitle = request.goals().stream()
                .map(GoalRequest.GoalItem::title)
                .anyMatch(title -> !requestedTitles.add(title));
        if (hasDuplicateTitle) {
            throw new InvalidOnboardingRequestException("목표 제목은 중복될 수 없습니다.");
        }

        List<DailyGoal> existingGoals = dailyGoalRepository.findAllByUserId(userId);
        existingGoals.forEach(DailyGoal::deactivate);

        Map<String, DailyGoal> existingGoalsByTitle = existingGoals.stream()
                .collect(Collectors.toMap(
                        DailyGoal::getDailyGoal,
                        Function.identity(),
                        (existing, duplicate) -> existing));

        // 1. 치료 목표 저장
        List<DailyGoal> goals = request.goals().stream()
                .map(item -> {
                    DailyGoal existingGoal = existingGoalsByTitle.get(item.title());
                    if (existingGoal != null) {
                        existingGoal.updateType(item.type());
                        existingGoal.reactivate();
                        return existingGoal;
                    }
                    return DailyGoal.builder()
                            .userId(userId)
                            .dailyGoal(item.title())
                            .type(item.type())
                            .isActive(true)
                            .build();
                })
                .toList();
        List<DailyGoal> saved = dailyGoalRepository.saveAll(goals);

        // 2. 태그 visible 업데이트
        if (request.visibleTagIds() != null) {
            Set<Long> visibleIds = new HashSet<>(request.visibleTagIds());
            troubleTagRepository.findAllByUserIdAndIsActiveTrue(userId)
                    .forEach(tag -> tag.changeVisibility(visibleIds.contains(tag.getId())));
        }

        List<GoalResponse.GoalItem> items = saved.stream()
                .map(g -> new GoalResponse.GoalItem(g.getId(), g.getDailyGoal(), g.getType(), g.isActive()))
                .toList();

        return new GoalResponse(items);
    }

    @Transactional
    public CompleteOnboardingResponse completeOnboarding(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        java.util.Optional<OnboardingSymptom> latestSymptom =
                onboardingSymptomRepository.findTopByUserOrderBySavedAtDesc(user);
        boolean hasSymptom = latestSymptom.isPresent();
        boolean hasGoals = dailyGoalRepository.existsByUserId(userId);

        // 경로 B(빠른 온보딩)는 ASRS 불필요
        boolean isQuickOnboarding = latestSymptom.map(OnboardingSymptom::isQuickOnboarding).orElse(false);
        boolean hasAsrs = isQuickOnboarding || asrsAssessmentRepository.existsByUser(user);

        if (!hasSymptom || !hasGoals || !hasAsrs) {
            throw new OnboardingNotCompleteException();
        }

        LocalDateTime now = LocalDateTime.now();
        user.completeOnboarding(now);
        return new CompleteOnboardingResponse(true, now);
    }

    @Transactional(readOnly = true)
    public OnboardingHistoryResponse getHistory(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        long goalCount = dailyGoalRepository.countByUserIdAndIsActiveTrue(userId);

        List<OnboardingHistoryResponse.HistoryRecord> records = asrsAssessmentRepository
                .findAllByUserWithAnswers(user)
                .stream()
                .map(a -> {
                    int inattentionScore = a.getAnswers().stream()
                            .filter(ans -> ans.getQuestionId() >= 1 && ans.getQuestionId() <= 9)
                            .mapToInt(AsrsAnswer::getScore)
                            .sum();
                    int hyperactivityScore = a.getAnswers().stream()
                            .filter(ans -> ans.getQuestionId() >= 10 && ans.getQuestionId() <= 18)
                            .mapToInt(AsrsAnswer::getScore)
                            .sum();
                    return new OnboardingHistoryResponse.HistoryRecord(
                            String.valueOf(a.getId()),
                            a.getCompletedAt(),
                            inattentionScore,
                            hyperactivityScore,
                            (int) goalCount
                    );
                })
                .toList();

        return new OnboardingHistoryResponse(records);
    }

    @Transactional(readOnly = true)
    public OnboardingHistoryDetailResponse getHistoryDetail(UUID userId, Long assessmentId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        AsrsAssessment assessment = asrsAssessmentRepository.findById(assessmentId)
                .filter(a -> a.getUser().getId().equals(user.getId()))
                .orElseThrow(AsrsAssessmentNotFoundException::new);

        int inattentionScore = assessment.getAnswers().stream()
                .filter(a -> a.getQuestionId() >= 1 && a.getQuestionId() <= 9)
                .mapToInt(AsrsAnswer::getScore)
                .sum();
        int hyperactivityScore = assessment.getAnswers().stream()
                .filter(a -> a.getQuestionId() >= 10 && a.getQuestionId() <= 18)
                .mapToInt(AsrsAnswer::getScore)
                .sum();

        OnboardingHistoryDetailResponse.SymptomDetail symptomDetail = onboardingSymptomRepository
                .findTopByUserAndSavedAtLessThanEqualOrderBySavedAtDesc(user, assessment.getCompletedAt())
                .map(s -> {
                    List<String> symptomTypes = s.getSelectedSymptomTypes() != null
                            ? List.of(s.getSelectedSymptomTypes().split(","))
                            : null;
                    List<String> functionalAreas = s.getSelectedFunctionalAreas() != null
                            ? List.of(s.getSelectedFunctionalAreas().split(","))
                            : null;
                    return new OnboardingHistoryDetailResponse.SymptomDetail(
                            s.getDescription(),
                            s.getEmotionalEvent(),
                            s.isQuickOnboarding(),
                            symptomTypes,
                            functionalAreas
                    );
                })
                .orElse(null);

        List<OnboardingHistoryDetailResponse.GoalItem> goals = dailyGoalRepository
                .findAllByUserIdAndIsActiveTrue(userId)
                .stream()
                .map(g -> new OnboardingHistoryDetailResponse.GoalItem(
                        g.getId(),
                        g.getDailyGoal(),
                        g.getType() != null ? g.getType().name() : null))
                .toList();

        return new OnboardingHistoryDetailResponse(
                String.valueOf(assessment.getId()),
                assessment.getCompletedAt(),
                inattentionScore,
                hyperactivityScore,
                symptomDetail,
                goals
        );
    }

    private DailyGoalType mapFunctionalArea(String koreanArea) {
        if (koreanArea == null || koreanArea.isBlank()) {
            throw new attune.common.error.internalserver.GeminiGenerationException(
                    "functionalArea is null or blank in Gemini response");
        }
        String area = koreanArea.trim();
        if (area.contains("업무") || area.contains("학업")) return DailyGoalType.WORK_STUDY;
        if (area.contains("시간")) return DailyGoalType.TIME_MANAGEMENT;
        if (area.contains("생활")) return DailyGoalType.LIFE_MANAGEMENT;
        if (area.contains("정서") || area.contains("관계")) return DailyGoalType.EMOTIONAL_SOCIAL;
        throw new attune.common.error.internalserver.GeminiGenerationException(
                "Unexpected functionalArea from Gemini: " + koreanArea);
    }
}
