package attune.medicationAnalysis.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Component
public class GeminiResponseValidator {

    private static final List<Pattern> FORBIDDEN_PATTERNS = List.of(
            Pattern.compile("약효가\\s*떨어"),
            Pattern.compile("복용량을\\s*(늘리|줄이|바꾸|변경)"),
            Pattern.compile("부작용이므로\\s*(복용을\\s*)?중단"),
            Pattern.compile("(특정\\s*)?질환으로\\s*진단"),
            Pattern.compile("처방을\\s*(받으|바꾸|변경)")
    );

    private final ObjectMapper objectMapper = buildObjectMapper();

    /**
     * JSON schema 검증 → evidenceId 존재 검증 → 금지 표현 검증 순서로 실행.
     * 검증 통과 시 파싱된 응답 반환.
     */
    public GeminiReportResponse validate(String rawJson, List<String> validEvidenceIds) {
        GeminiReportResponse response = parseJson(rawJson);
        validateEvidenceIds(response, Set.copyOf(validEvidenceIds));
        validateForbiddenExpressions(rawJson);
        return response;
    }

    private GeminiReportResponse parseJson(String rawJson) {
        try {
            String cleaned = cleanMarkdownFence(rawJson);
            return objectMapper.readValue(cleaned, GeminiReportResponse.class);
        } catch (Exception e) {
            throw new GeminiValidationException("JSON 파싱 실패: " + e.getMessage());
        }
    }

    private void validateEvidenceIds(GeminiReportResponse response, Set<String> validEvidenceIds) {
        if (response.insights() == null) return;
        for (GeminiReportResponse.Insight insight : response.insights()) {
            if (insight.evidenceIds() == null) continue;
            for (String evidenceId : insight.evidenceIds()) {
                if (!validEvidenceIds.contains(evidenceId)) {
                    throw new GeminiValidationException("스냅샷에 존재하지 않는 evidenceId: " + evidenceId);
                }
            }
        }
    }

    private void validateForbiddenExpressions(String text) {
        for (Pattern pattern : FORBIDDEN_PATTERNS) {
            if (pattern.matcher(text).find()) {
                throw new GeminiValidationException("금지 표현 감지: " + pattern.pattern());
            }
        }
    }

    private String cleanMarkdownFence(String raw) {
        String trimmed = raw.strip();
        int firstFence = trimmed.indexOf("```");
        if (firstFence >= 0) {
            int firstNewline = trimmed.indexOf('\n', firstFence);
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).strip();
            }
        }
        return trimmed;
    }

    private ObjectMapper buildObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
