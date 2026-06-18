package attune.medicationAnalysis.infrastructure;

import attune.ai.application.AiTextGenerator;
import attune.ai.config.GeminiProperties;
import attune.medicationAnalysis.application.model.AnalysisSnapshot;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GeminiReportClientTest {

    private final AiTextGenerator aiTextGenerator = mock(AiTextGenerator.class);
    private final GeminiResponseValidator validator = mock(GeminiResponseValidator.class);
    private final GeminiProperties properties = new GeminiProperties(null, null, "gemini-test");
    private final GeminiReportClient client = new GeminiReportClient(aiTextGenerator, properties, validator);

    @Test
    void generate_buildsPromptFromResourceTemplate() {
        String snapshotJson = "{\"period\":{\"days\":7}}";
        AnalysisSnapshot snapshot = mock(AnalysisSnapshot.class);
        AnalysisSnapshot.TimeWindowPattern pattern = new AnalysisSnapshot.TimeWindowPattern(
                "오전", "TIME_WINDOW_01", Map.of(), Map.of(), Map.of(), 3);

        when(snapshot.timeWindowPatterns()).thenReturn(List.of(pattern));
        when(aiTextGenerator.generateJson(anyString())).thenReturn("{}");
        when(validator.validate("{}", snapshotJson))
                .thenReturn(new GeminiReportResponse("요약", List.of(), List.of(), "안내"));

        GeminiReportClient.GeminiReportResult result = client.generate(snapshotJson, snapshot);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiTextGenerator).generateJson(promptCaptor.capture());
        String prompt = promptCaptor.getValue();

        assertTrue(result.success());
        assertTrue(prompt.contains("당신은 ADHD 약물 치료를 받는 사용자의"));
        assertTrue(prompt.contains("TIME_WINDOW_01"));
        assertTrue(prompt.contains(snapshotJson));
        assertFalse(prompt.contains("{{VALID_EVIDENCE_IDS}}"));
        assertFalse(prompt.contains("{{SNAPSHOT_JSON}}"));
    }
}
