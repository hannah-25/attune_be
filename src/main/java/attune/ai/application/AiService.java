package attune.ai.application;

import attune.ai.application.dto.request.GenerateTextRequest;
import attune.ai.application.dto.response.GenerateTextResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AiService {

    private final AiTextGenerator aiTextGenerator;

    public GenerateTextResponse generate(GenerateTextRequest request) {
        String text = aiTextGenerator.generate(request.prompt());
        return new GenerateTextResponse(text);
    }
}
