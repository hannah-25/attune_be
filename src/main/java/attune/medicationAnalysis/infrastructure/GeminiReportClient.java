package attune.medicationAnalysis.infrastructure;

import attune.ai.application.AiTextGenerator;
import attune.ai.config.GeminiProperties;
import attune.medicationAnalysis.application.model.AnalysisSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiReportClient {

    private static final String PROMPT_VERSION = "v1.0";

    private final AiTextGenerator aiTextGenerator;
    private final GeminiProperties geminiProperties;
    private final GeminiResponseValidator validator;

    private final ObjectMapper objectMapper = buildObjectMapper();

    public GeminiReportResult generate(String snapshotJson, AnalysisSnapshot snapshot) {
        List<String> validEvidenceIds = snapshot.timeWindowPatterns().stream()
                .map(AnalysisSnapshot.TimeWindowPattern::evidenceId)
                .toList();
        String prompt = buildPrompt(snapshotJson, validEvidenceIds);
        try {
            String rawJson = aiTextGenerator.generateJson(prompt);
            GeminiReportResponse parsed = validator.validate(rawJson, snapshotJson);
            return GeminiReportResult.success(rawJson, geminiProperties.model(), PROMPT_VERSION);
        } catch (GeminiValidationException e) {
            log.warn("Gemini 응답 검증 실패: {}", e.getMessage());
            return GeminiReportResult.failed(geminiProperties.model(), PROMPT_VERSION);
        } catch (Exception e) {
            log.warn("Gemini 호출 실패: {}", e.getMessage());
            return GeminiReportResult.failed(geminiProperties.model(), PROMPT_VERSION);
        }
    }

    public String getPromptVersion() {
        return PROMPT_VERSION;
    }

    private String buildPrompt(String snapshotJson, List<String> validEvidenceIds) {
        String evidenceIdList = validEvidenceIds.isEmpty()
                ? "(없음)"
                : String.join(", ", validEvidenceIds);

        return """
                당신은 ADHD 약물 치료를 받는 사용자의 복용 기록과 일지 데이터를 분석하여,
                기록된 패턴을 사용자가 이해하기 쉬운 언어로 요약하는 역할을 합니다.

                중요한 제약사항:
                - 약효를 판단하거나 의료적 조언을 제공하지 않습니다.
                - 인과관계를 단정하지 않습니다. "~때문에"가 아닌 "~경향이 있었어요" 표현을 사용합니다.
                - 서버가 계산한 수치를 벗어나는 새로운 통계를 생성하지 않습니다.
                - 금지 표현: "약효가 떨어졌어요", "복용량을 늘리세요", "부작용이므로 중단하세요", "특정 질환으로 진단됩니다"
                - evidenceIds는 반드시 아래 목록에서만 선택하세요:
                """ + evidenceIdList + """

                다음 JSON 분석 스냅샷을 바탕으로 아래 형식의 JSON을 반환하세요:

                {
                  "summary": "선택 기간의 기록 요약 (2-3문장)",
                  "insights": [
                    {
                      "category": "ADHERENCE | DAY_GROUP | TIME_PATTERN | SIDE_EFFECT | MEDICATION_CHANGE",
                      "title": "패턴 제목 (15자 이내)",
                      "description": "구체적인 설명 (50자 이내, 수치 포함)",
                      "evidenceIds": ["위 목록의 ID만 사용"],
                      "confidence": "HIGH | MEDIUM | LOW",
                      "limitation": "해석 한계 설명"
                    }
                  ],
                  "consultationQuestions": [
                    "상담 시 의료진에게 확인할 질문 (최대 3개)"
                  ],
                  "disclaimer": "기록된 패턴을 요약한 내용이며 의료적 판단이 아닙니다."
                }

                insights는 최대 3개, consultationQuestions는 최대 3개입니다.
                데이터가 부족한 항목은 insights에서 제외하세요.

                --- 분석 스냅샷 ---
                """ + snapshotJson;
    }

    private ObjectMapper buildObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    public record GeminiReportResult(
            boolean success,
            String aiResultJson,
            String modelName,
            String promptVersion
    ) {
        static GeminiReportResult success(String json, String model, String version) {
            return new GeminiReportResult(true, json, model, version);
        }

        static GeminiReportResult failed(String model, String version) {
            return new GeminiReportResult(false, null, model, version);
        }
    }
}
