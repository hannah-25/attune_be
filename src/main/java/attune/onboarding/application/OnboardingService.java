package attune.onboarding.application;

import attune.common.error.badrequest.InvalidOnboardingRequestException;
import attune.common.error.badrequest.OnboardingNotCompleteException;
import attune.common.error.notfound.AsrsAssessmentNotFoundException;
import attune.common.error.notfound.UserNotFoundException;
import attune.journal.application.JournalTagService;
import attune.journal.domain.model.DailyGoal;
import attune.journal.domain.model.DailyGoalType;
import attune.journal.domain.model.JournalTagCategory;
import attune.journal.domain.model.OnboardingGoalSnapshot;
import attune.journal.domain.repository.DailyGoalRepository;
import attune.journal.domain.repository.OnboardingGoalSnapshotRepository;
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

import java.text.Normalizer;
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
    private final OnboardingGoalSnapshotRepository onboardingGoalSnapshotRepository;
    private final OnboardingAiService onboardingAiService;
    private final JournalTagService journalTagService;

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

        // 문제 상황(TROUBLE) 태그: 시스템 태그 전체 + Gemini 추천 여부 표시
        Set<String> recommendedTrouble = toNameSet(geminiResponse.visibleTags());
        List<AiRecommendationResponse.TagItem> tagItems = journalTagService.getTroubleTags(userId).stream()
                .map(t -> new AiRecommendationResponse.TagItem(
                        t.tagId(), t.name(), t.tagType(),
                        recommendedTrouble.contains(t.name())))
                .toList();

        // 감정·컨디션(CONDITION) 태그: 시스템 태그 전체 + Gemini 추천 여부 표시
        Set<String> recommendedCondition = toNameSet(geminiResponse.visibleConditionTags());
        List<AiRecommendationResponse.ConditionTagItem> conditionItems = journalTagService.getConditionTags(userId).stream()
                .map(t -> new AiRecommendationResponse.ConditionTagItem(
                        t.tagId(), t.name(), t.tagType(),
                        recommendedCondition.contains(t.name())))
                .toList();

        // 목표: Korean functionalArea → DailyGoalType 매핑
        List<AiRecommendationResponse.GoalItem> goalItems = geminiResponse.treatmentGoals().stream()
                .map(g -> new AiRecommendationResponse.GoalItem(g.goal(), mapFunctionalArea(g.functionalArea())))
                .toList();

        return new AiRecommendationResponse(tagItems, conditionItems, goalItems);
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

        LocalDateTime now = LocalDateTime.now();

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
                            .savedAt(now)
                            .build();
                })
                .toList();
        List<DailyGoal> saved = dailyGoalRepository.saveAll(goals);

        List<OnboardingGoalSnapshot> snapshots = saved.stream()
                .map(g -> OnboardingGoalSnapshot.builder()
                        .userId(userId)
                        .dailyGoal(g)
                        .onboardingTime(now)
                        .build())
                .toList();
        onboardingGoalSnapshotRepository.saveAll(snapshots);

        // 2. 태그 visible 업데이트 (필드 생략(null) 시 변경 안 함 / 빈 배열이면 전체 숨김)
        if (request.visibleTagIds() != null) {
            journalTagService.bulkSetVisibilityForOnboarding(
                    userId, JournalTagCategory.TROUBLE, new HashSet<>(request.visibleTagIds()));
        }
        if (request.visibleConditionTagIds() != null) {
            journalTagService.bulkSetVisibilityForOnboarding(
                    userId, JournalTagCategory.CONDITION, new HashSet<>(request.visibleConditionTagIds()));
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
        long goalCount = dailyGoalRepository.countByUserIdAndIsActiveTrue(userId);

        List<OnboardingHistoryResponse.HistoryRecord> records = asrsAssessmentRepository
                .findAllByUserWithAnswers(userId)
                .stream()
                .map(a -> new OnboardingHistoryResponse.HistoryRecord(
                        String.valueOf(a.getId()),
                        a.getCompletedAt(),
                        calcInattentionScore(a),
                        calcHyperactivityScore(a),
                        (int) goalCount
                ))
                .toList();

        return new OnboardingHistoryResponse(records);
    }

    @Transactional(readOnly = true)
    public OnboardingHistoryDetailResponse getHistoryDetail(UUID userId, Long assessmentId) {
        AsrsAssessment assessment = asrsAssessmentRepository
                .findByIdAndUserWithAnswers(assessmentId, userId)
                .orElseThrow(AsrsAssessmentNotFoundException::new);

        int inattentionScore = calcInattentionScore(assessment);
        int hyperactivityScore = calcHyperactivityScore(assessment);

        OnboardingHistoryDetailResponse.SymptomDetail symptomDetail = onboardingSymptomRepository
                .findTopByUserIdAndSavedAtLessThanEqualOrderBySavedAtDesc(userId, assessment.getCompletedAt())
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

        LocalDateTime assessmentTime = assessment.getCompletedAt();
        LocalDateTime nextAssessmentTime = asrsAssessmentRepository
                .findFirstByUser_IdAndCompletedAtAfterOrderByCompletedAtAsc(userId, assessmentTime)
                .map(AsrsAssessment::getCompletedAt)
                .orElse(null);

        List<DailyGoal> goalsAtTime = nextAssessmentTime != null
                ? onboardingGoalSnapshotRepository.findGoalsByUserAndTimeBetween(userId, assessmentTime, nextAssessmentTime)
                : onboardingGoalSnapshotRepository.findGoalsByUserAndTimeFrom(userId, assessmentTime);

        List<OnboardingHistoryDetailResponse.GoalItem> goals = goalsAtTime.stream()
                .map(g -> new OnboardingHistoryDetailResponse.GoalItem(
                        g.getId(),
                        g.getDailyGoal(),
                        g.getType()))
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

    private int calcInattentionScore(AsrsAssessment assessment) {
        return assessment.getAnswers().stream()
                .filter(a -> a.getQuestionId() >= 1 && a.getQuestionId() <= 9)
                .mapToInt(AsrsAnswer::getScore)
                .sum();
    }

    private int calcHyperactivityScore(AsrsAssessment assessment) {
        return assessment.getAnswers().stream()
                .filter(a -> a.getQuestionId() >= 10 && a.getQuestionId() <= 18)
                .mapToInt(AsrsAnswer::getScore)
                .sum();
    }

    private static Set<String> toNameSet(List<String> names) {
        if (names == null) {
            return Set.of();
        }
        // Gemini가 공백을 포함하거나 NFD로 응답해도 NFC 시스템 태그명과 매칭되도록 정규화한다.
        return names.stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .map(name -> Normalizer.normalize(name, Normalizer.Form.NFC))
                .collect(Collectors.toSet());
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
