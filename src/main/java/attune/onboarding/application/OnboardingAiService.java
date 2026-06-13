package attune.onboarding.application;

import attune.common.error.internalserver.GeminiGenerationException;
import attune.ai.application.AiTextGenerator;
import attune.journal.domain.model.DailyGoalType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OnboardingAiService {

    private final AiTextGenerator aiTextGenerator;
    private final ObjectMapper objectMapper;

    private static final String TAG_POOL = """
            부주의: 딴생각, 깜빡함, 설명을 놓침, 중요한 내용을 빠뜨림, 집중이 끊김
            시간관리: 미룸, 시작이 어려움, 지각함, 예상보다 오래 걸림, 우선순위 못 정함
            충동성: 급하게 처리, 확인 전 제출함, 감정적으로 반응함
            과활성: 가만히 있기 어려움, 말이 많아짐
            인지오류: 숫자/단위 실수, 날짜/일정 착각, 항목 혼동, 순서를 헷갈림""";

    private static final String FUNCTIONAL_AREAS = "업무/학업, 시간관리, 생활관리, 정서/관계";

    private static final Map<DailyGoalType, String> FUNCTIONAL_AREA_LABELS = Map.of(
            DailyGoalType.WORK_STUDY,        "업무/학업",
            DailyGoalType.TIME_MANAGEMENT,   "시간관리",
            DailyGoalType.LIFE_MANAGEMENT,   "생활관리",
            DailyGoalType.EMOTIONAL_SOCIAL,  "정서/관계"
    );

    private static final String OUTPUT_RULES = """
            [출력 규칙]
            - visibleTags: 사용자와 가장 관련 높은 태그 5~7개를 태그 풀에서만 선택
            - treatmentGoals: 4개, 매일 0~10점으로 평가 가능한 행동 목표, 증상이 아닌 기능 회복 중심, 자연스러운 한국어
            - 다른 텍스트 없이 아래 형식의 JSON만 출력:
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
                부주의: %d점 (부주의 점수가 높을 경우 시간관리 어려움도 함께 있는 것으로 해석하세요)
                과잉행동-충동성: %d점
                
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
}
