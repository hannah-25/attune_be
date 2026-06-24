package attune.ai.adapter.gemini;

import attune.ai.application.AiTextGenerator;
import attune.ai.config.GeminiProperties;
import attune.common.error.internalserver.GeminiGenerationException;
import attune.common.error.serviceunavailable.GeminiUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Component
public class GeminiTextGenerator implements AiTextGenerator {

    private static final String GENERATION_FAILED_MESSAGE = "Gemini response generation failed.";

    /** 전이성(일시적) 오류로 보고 재시도하는 HTTP 상태 코드. */
    private static final Set<Integer> RETRYABLE_STATUS = Set.of(429, 500, 503);
    private static final int MAX_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 200L;

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

        RuntimeException lastTransient = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
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
            } catch (RestClientResponseException e) {
                if (!RETRYABLE_STATUS.contains(e.getStatusCode().value())) {
                    // 400/401/403/404 등은 재시도해도 의미 없으므로 즉시 실패.
                    throw new GeminiGenerationException(GENERATION_FAILED_MESSAGE, e);
                }
                lastTransient = e;
                log.warn("Gemini 일시적 오류(HTTP {}) — 재시도 {}/{}",
                        e.getStatusCode().value(), attempt, MAX_ATTEMPTS);
            } catch (ResourceAccessException e) {
                lastTransient = e;
                log.warn("Gemini 연결 실패 — 재시도 {}/{}: {}", attempt, MAX_ATTEMPTS, e.getMessage());
            }

            if (attempt < MAX_ATTEMPTS) {
                backoff(attempt);
            }
        }

        // 전이성 오류로 재시도를 모두 소진 → 503으로 매핑해 클라이언트가 잠시 후 재시도하도록 한다.
        throw new GeminiUnavailableException(GENERATION_FAILED_MESSAGE, lastTransient);
    }

    private void backoff(int attempt) {
        long delay = INITIAL_BACKOFF_MS * (1L << (attempt - 1)); // 200ms → 400ms → ...
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new GeminiUnavailableException(GENERATION_FAILED_MESSAGE, ie);
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
