package attune.medicationAnalysis.infrastructure;

import attune.ai.application.AiTextGenerator;
import attune.ai.config.GeminiProperties;
import attune.medicationAnalysis.application.model.AnalysisSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiReportClient {

    private static final String PROMPT_VERSION = "v1.0";
    private static final String PROMPT_TEMPLATE_PATH = "prompts/medication-analysis-report-v1.0.txt";
    private static final String EVIDENCE_IDS_PLACEHOLDER = "{{VALID_EVIDENCE_IDS}}";
    private static final String SNAPSHOT_JSON_PLACEHOLDER = "{{SNAPSHOT_JSON}}";
    private static final String PROMPT_TEMPLATE = loadPromptTemplate();

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

        return PROMPT_TEMPLATE
                .replace(EVIDENCE_IDS_PLACEHOLDER, evidenceIdList)
                .replace(SNAPSHOT_JSON_PLACEHOLDER, snapshotJson);
    }

    private static String loadPromptTemplate() {
        try {
            return new ClassPathResource(PROMPT_TEMPLATE_PATH)
                    .getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("프롬프트 템플릿을 읽을 수 없습니다: " + PROMPT_TEMPLATE_PATH, e);
        }
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
