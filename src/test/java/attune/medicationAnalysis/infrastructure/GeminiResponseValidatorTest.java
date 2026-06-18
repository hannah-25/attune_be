package attune.medicationAnalysis.infrastructure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeminiResponseValidatorTest {

    private final GeminiResponseValidator validator = new GeminiResponseValidator();

    private static final String VALID_SNAPSHOT = "{\"evidenceId\":\"TIME_WINDOW_01\"}";

    private static final String VALID_JSON = """
            {
              "summary": "기록 요약입니다.",
              "insights": [
                {
                  "category": "ADHERENCE",
                  "title": "복용 패턴",
                  "description": "10일 중 8일 복용했습니다.",
                  "evidenceIds": ["TIME_WINDOW_01"],
                  "confidence": "HIGH",
                  "limitation": "기록이 적습니다."
                }
              ],
              "consultationQuestions": ["선생님께 여쭤볼 점이 있습니다."],
              "disclaimer": "기록된 패턴을 요약한 내용이며 의료적 판단이 아닙니다."
            }
            """;

    @Test
    void validate_passes_validResponse() {
        assertDoesNotThrow(() -> validator.validate(VALID_JSON, VALID_SNAPSHOT));
    }

    @Test
    void validate_throws_whenJsonIsMalformed() {
        assertThrows(GeminiValidationException.class,
                () -> validator.validate("not-json", VALID_SNAPSHOT));
    }

    @Test
    void validate_throws_whenEvidenceIdNotInSnapshot() {
        String jsonWithBadId = VALID_JSON.replace("TIME_WINDOW_01", "NONEXISTENT_ID");

        assertThrows(GeminiValidationException.class,
                () -> validator.validate(jsonWithBadId, VALID_SNAPSHOT));
    }

    @Test
    void validate_throws_whenForbiddenExpression_약효가떨어() {
        String forbidden = VALID_JSON.replace("복용 패턴", "약효가 떨어졌어요");

        assertThrows(GeminiValidationException.class,
                () -> validator.validate(forbidden, VALID_SNAPSHOT));
    }

    @Test
    void validate_throws_whenForbiddenExpression_복용량늘리() {
        String forbidden = VALID_JSON.replace("기록된 패턴을 요약한 내용이며 의료적 판단이 아닙니다.",
                "복용량을 늘리세요.");

        assertThrows(GeminiValidationException.class,
                () -> validator.validate(forbidden, VALID_SNAPSHOT));
    }

    @Test
    void validate_stripsMarkdownFence_beforeParsing() {
        String fenced = "```json\n" + VALID_JSON + "\n```";

        assertDoesNotThrow(() -> validator.validate(fenced, VALID_SNAPSHOT));
    }

    @Test
    void validate_passes_whenInsightsIsNull() {
        String noInsights = """
                {
                  "summary": "요약",
                  "insights": null,
                  "consultationQuestions": [],
                  "disclaimer": "의료적 판단이 아닙니다."
                }
                """;

        assertDoesNotThrow(() -> validator.validate(noInsights, VALID_SNAPSHOT));
    }
}
