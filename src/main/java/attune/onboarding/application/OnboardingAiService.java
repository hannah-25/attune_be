package attune.onboarding.application;

import attune.common.error.internalserver.GeminiGenerationException;
import attune.ai.application.AiTextGenerator;
import attune.journal.domain.model.DailyGoalType;
import attune.journal.domain.model.SystemJournalTagDefinitions;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OnboardingAiService {

    private final AiTextGenerator aiTextGenerator;
    private final ObjectMapper objectMapper;

    private static final Map<String, String> TROUBLE_TYPE_LABELS = Map.of(
            "INATTENTION", "부주의",
            "TIME_MANAGEMENT", "시간관리",
            "IMPULSIVITY", "충동성",
            "HYPERACTIVITY", "과활성",
            "COGNITIVE_ERROR", "인지오류"
    );

    private static final String TAG_POOL = buildTagPool();

    private static final String FUNCTIONAL_AREAS = "업무/학업, 시간관리, 생활관리, 정서/관계";

    private static final Map<DailyGoalType, String> FUNCTIONAL_AREA_LABELS = Map.of(
            DailyGoalType.WORK_STUDY,        "업무/학업",
            DailyGoalType.TIME_MANAGEMENT,   "시간관리",
            DailyGoalType.LIFE_MANAGEMENT,   "생활관리",
            DailyGoalType.EMOTIONAL_SOCIAL,  "정서/관계"
    );

    private static final String OUTPUT_RULES = """
            [출력 규칙]
            - visibleTags: 사용자와 가장 관련 높은 세부 태그 5~7개를 태그 풀의 각 줄 콜론(:) 뒤에 나열된 항목 중에서만 선택. '부주의', '시간관리', '충동성', '과활성', '인지오류' 같은 카테고리명은 절대 선택 불가.
            - treatmentGoals: 반드시 4개 고정.

            [treatmentGoals 작성 가이드라인 - 엄격 적용]
            종결 어미: 문장은 무조건 명사형 어미 '~하기', '~챙기기', '~두기'로만 끝낼 것.
            글자 수 제약: 인지적 부하를 줄이기 위해 글자 수는 10자 내외(최대 15자 이하), 어절은 3~4개 이내로 극도의 간결함을 유지할 것.
            압박감 배제: '철저히', '엄수', '반드시', '노력하기' 같은 압박감을 주거나 모호한 단어는 절대 금지. 담담하고 직관적인 행동만 기술할 것.
            행동의 최소화(Micro-Action): 거창한 목표가 아니라, 당장 1초 뒤에 실행할 수 있는 '첫 번째 행동' 중심일 것.

            좋은 예시: "알람 울리면 바로 시작하기", "아침에 할 일 3개만 적기", "외출 물건 문 앞에 두기"
            나쁜 예시: "알람을 활용하여 과제 시작 시간을 철저히 엄수하기", "매일 아침 할 일 목록을 작성하고 시간 관리 잘하기"

            다른 텍스트 없이 아래 형식의 JSON만 출력:
            {
              "visibleTags": ["태그명1", "태그명2"],
              "treatmentGoals": [
                {"goal": "목표 문장", "functionalArea": "업무/학업"},
                {"goal": "목표 문장", "functionalArea": "시간관리"}
              ]
            }""";

    /**
     * 경로 A: 증상 서술 + ASRS 결과 기반 분석
     */
    public GeminiOnboardingResponse analyzeFullOnboarding(
            String symptomDescription,
            int inattentionScore,
            int hyperactivityScore) {

        String prompt = """
                당신은 성인 ADHD 환자의 온보딩 데이터를 분석하는 전문가입니다.
                
                [사용자 증상 서술]
                %s
                
                [ASRS 검사 결과]
                주의력 점수: %d점 (높을 경우 시간관리 어려움도 함께 있는 것으로 해석하세요)
                과잉행동·충동 점수: %d점
                
                [태그 풀 - 반드시 이 목록에서만 선택]
                %s
                
                [functionalArea 가능한 값]
                %s
                
                %s
                """.formatted(symptomDescription, inattentionScore, hyperactivityScore,
                TAG_POOL, FUNCTIONAL_AREAS, OUTPUT_RULES);

        return parse(aiTextGenerator.generateJson(prompt));
    }

    /**
     * 경로 B: 이미 진단받은 사용자의 취약 영역 직접 선택 기반 분석
     */
    public GeminiOnboardingResponse analyzeQuickOnboarding(
            List<String> selectedSymptomTypes,
            List<DailyGoalType> selectedFunctionalAreas) {

        String functionalAreaLabels = selectedFunctionalAreas.stream()
                .map(t -> FUNCTIONAL_AREA_LABELS.getOrDefault(t, t.name()))
                .collect(java.util.stream.Collectors.joining(", "));

        String prompt = """
                당신은 성인 ADHD 환자의 온보딩 데이터를 분석하는 전문가입니다.

                [사용자가 선택한 취약 증상 영역]
                %s

                [사용자가 선택한 취약 기능 영역]
                %s

                [태그 풀 - 반드시 이 목록에서만 선택]
                %s

                [functionalArea 가능한 값]
                %s

                %s
                """.formatted(
                String.join(", ", selectedSymptomTypes),
                functionalAreaLabels,
                TAG_POOL, FUNCTIONAL_AREAS, OUTPUT_RULES);

        return parse(aiTextGenerator.generateJson(prompt));
    }

    private GeminiOnboardingResponse parse(String json) {
        if (json == null || json.isBlank()) {
            throw new GeminiGenerationException("Empty response from Gemini");
        }
        try {
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}');
            if (start == -1 || end == -1 || start > end) {
                throw new GeminiGenerationException("No valid JSON object found in Gemini response");
            }
            return objectMapper.readValue(json.substring(start, end + 1), GeminiOnboardingResponse.class);
        } catch (GeminiGenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new GeminiGenerationException("Failed to parse Gemini onboarding response", e);
        }
    }

    private static String buildTagPool() {
        Map<String, List<String>> namesByType = SystemJournalTagDefinitions.troubleTags().stream()
                .collect(Collectors.groupingBy(
                        SystemJournalTagDefinitions.Definition::tagType,
                        LinkedHashMap::new,
                        Collectors.mapping(SystemJournalTagDefinitions.Definition::name, Collectors.toList())
                ));
        return namesByType.entrySet().stream()
                .map(entry -> Objects.requireNonNull(
                        TROUBLE_TYPE_LABELS.get(entry.getKey()),
                        "TROUBLE_TYPE_LABELS missing entry for tagType: " + entry.getKey())
                        + ": " + String.join(", ", entry.getValue()))
                .collect(Collectors.joining("\n"));
    }
}
