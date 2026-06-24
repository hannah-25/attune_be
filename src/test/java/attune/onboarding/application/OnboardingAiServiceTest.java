package attune.onboarding.application;

import attune.ai.application.AiTextGenerator;
import attune.journal.domain.model.SystemJournalTagDefinitions;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OnboardingAiServiceTest {

    @Test
    void promptUsesCanonicalSystemTroubleTagNames() {
        AiTextGenerator aiTextGenerator = mock(AiTextGenerator.class);
        OnboardingAiService service = new OnboardingAiService(
                aiTextGenerator, new ObjectMapper());
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        when(aiTextGenerator.generateJson(promptCaptor.capture()))
                .thenReturn("""
                        {
                          "visibleTags": ["깜빡함"],
                          "visibleConditionTags": ["불안함"],
                          "treatmentGoals": []
                        }
                        """);

        service.analyzeFullOnboarding("집중이 어렵습니다.", 10, 5);

        String prompt = promptCaptor.getValue();
        assertThat(SystemJournalTagDefinitions.troubleTags())
                .allSatisfy(definition ->
                        assertThat(prompt).contains(definition.name()));
        assertThat(SystemJournalTagDefinitions.conditionTags())
                .allSatisfy(definition ->
                        assertThat(prompt).contains(definition.name()));
        verify(aiTextGenerator).generateJson(prompt);
    }
}
