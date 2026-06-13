package attune.ai.adapter.gemini;

import attune.ai.application.AiTextGenerator;
import attune.ai.config.GeminiProperties;
import attune.common.error.internalserver.GeminiGenerationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Objects;

@Component
public class GeminiTextGenerator implements AiTextGenerator {

    private static final String GENERATION_FAILED_MESSAGE = "Gemini response generation failed.";

    private final RestClient restClient;
    private final GeminiProperties properties;

    public GeminiTextGenerator(
            @Qualifier("geminiRestClient") RestClient restClient,
            GeminiProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public String generate(String prompt) {
        return callGemini(GeminiRequest.text(prompt));
    }

    @Override
    public String generateJson(String prompt) {
        return callGemini(GeminiRequest.json(prompt));
    }

    private String callGemini(GeminiRequest request) {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new GeminiGenerationException("GEMINI_API_KEY is not configured.");
        }

        try {
            GeminiResponse response = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent", properties.model())
                    .header("x-goog-api-key", properties.apiKey())
                    .body(request)
                    .retrieve()
                    .body(GeminiResponse.class);

            String text = extractText(response);
            if (text.isBlank()) {
                throw new GeminiGenerationException("Gemini returned an empty response.");
            }
            return text;
        } catch (RestClientResponseException | ResourceAccessException e) {
            throw new GeminiGenerationException(GENERATION_FAILED_MESSAGE, e);
        }
    }

    private String extractText(GeminiResponse response) {
        if (response == null || response.candidates() == null) {
            return "";
        }

        return response.candidates().stream()
                .map(Candidate::content)
                .filter(Objects::nonNull)
                .map(Content::parts)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .map(Part::text)
                .filter(Objects::nonNull)
                .filter(text -> !text.isBlank())
                .findFirst()
                .orElse("");
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record GeminiRequest(List<Content> contents, GenerationConfig generationConfig) {
        static GeminiRequest text(String prompt) {
            return new GeminiRequest(
                    List.of(new Content("user", List.of(new Part(prompt)))),
                    null
            );
        }

        static GeminiRequest json(String prompt) {
            return new GeminiRequest(
                    List.of(new Content("user", List.of(new Part(prompt)))),
                    new GenerationConfig("application/json")
            );
        }
    }

    private record GeminiResponse(List<Candidate> candidates) {}

    private record Candidate(Content content) {}

    private record Content(String role, List<Part> parts) {}

    private record Part(String text) {}

    private record GenerationConfig(String responseMimeType) {}
}
