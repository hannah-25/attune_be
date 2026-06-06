package attune.ai.adapter.gemini;

import attune.ai.application.AiTextGenerator;
import attune.ai.config.GeminiProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

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
        if (properties.apiKey().isBlank()) {
            throw new GeminiGenerationException("GEMINI_API_KEY is not configured.");
        }

        try {
            GeminiResponse response = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent", properties.model())
                    .header("x-goog-api-key", properties.apiKey())
                    .body(GeminiRequest.from(prompt))
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

    private record GeminiRequest(List<Content> contents) {
        private static GeminiRequest from(String prompt) {
            return new GeminiRequest(List.of(new Content("user", List.of(new Part(prompt)))));
        }
    }

    private record GeminiResponse(List<Candidate> candidates) {}

    private record Candidate(Content content) {}

    private record Content(String role, List<Part> parts) {}

    private record Part(String text) {}
}
