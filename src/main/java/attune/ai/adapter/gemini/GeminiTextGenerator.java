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

@Slf4j
@Component
public class GeminiTextGenerator implements AiTextGenerator {

    private static final String GENERATION_FAILED_MESSAGE = "Gemini response generation failed.";

    // 재시도는 503(과부하)·연결 끊김에만, 그것도 1회만 한다.
    // 429(쿼터/레이트리밋)는 재시도해도 회복되지 않고 호출 비용만 늘기 때문에 즉시 503으로 안내한다.
    private static final int MAX_ATTEMPTS = 2;          // 최초 1회 + 재시도 1회
    private static final long INITIAL_BACKOFF_MS = 300L;

    private final RestClient restClient;
    private final GeminiProperties properties;
    private final Sleeper sleeper;

    /** 백오프 대기 추상화. 운영에서는 {@link Thread#sleep(long)}, 테스트에서는 즉시 반환 구현을 주입한다. */
    @FunctionalInterface
    public interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    public GeminiTextGenerator(
            @Qualifier("geminiRestClient") RestClient restClient,
            GeminiProperties properties
    ) {
        this(restClient, properties, Thread::sleep);
    }

    GeminiTextGenerator(
            RestClient restClient,
            GeminiProperties properties,
            Sleeper sleeper
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.sleeper = sleeper;
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
                int status = e.getStatusCode().value();
                if (status == 429) {
                    // 쿼터/레이트리밋 초과 — 재시도해도 회복 안 됨(비용만 발생). 즉시 503 안내.
                    log.warn("Gemini 쿼터/레이트리밋 초과(429) — 재시도하지 않음");
                    throw new GeminiUnavailableException(GENERATION_FAILED_MESSAGE, e);
                }
                if (status != 502 && status != 503 && status != 504) {
                    // 400/401/403/404/500 등은 재시도 의미 없음 → 즉시 실패(500).
                    throw new GeminiGenerationException(GENERATION_FAILED_MESSAGE, e);
                }
                lastTransient = e; // 게이트웨이/과부하 일시 오류(502·503·504)만 재시도
                log.warn("Gemini 일시적 오류(HTTP {}) — 재시도 {}/{}", status, attempt, MAX_ATTEMPTS);
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
        long delay = INITIAL_BACKOFF_MS * (1L << (attempt - 1)); // 300ms → 600ms → ...
        try {
            sleeper.sleep(delay);
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
