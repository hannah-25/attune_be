package attune.ai.application;

import attune.ai.application.dto.request.GenerateTextRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    @Mock
    private AiTextGenerator aiTextGenerator;

    @InjectMocks
    private AiService aiService;

    @Test
    void generateReturnsGeneratedText() {
        given(aiTextGenerator.generate("summarize today")).willReturn("summary");

        var response = aiService.generate(new GenerateTextRequest("summarize today"));

        assertThat(response.text()).isEqualTo("summary");
    }
}
