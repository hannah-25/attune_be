package attune.ai.adapter.gemini;

import attune.ai.config.GeminiProperties;
import attune.common.error.internalserver.GeminiGenerationException;
import attune.common.error.serviceunavailable.GeminiUnavailableException;
import attune.common.observability.ObservabilityMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiTextGeneratorTest {

    private static final String BASE_URL = "http://gemini.test";
    private static final String URL =
            BASE_URL + "/v1beta/models/gemini-2.5-flash:generateContent";
    private static final String SUCCESS_BODY =
            "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"OK\"}]}}]}";
    private static final String SENSITIVE_ERROR_BODY =
            "{\"error\":\"사용자 복약 기록과 AI 응답 본문\"}";

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private GeminiTextGenerator generator;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        generator = new GeminiTextGenerator(
                builder.build(),
                new GeminiProperties("test-key", BASE_URL, "gemini-2.5-flash"),
                mock(ObservabilityMetrics.class),
                millis -> { /* 테스트에서는 백오프 대기 없이 즉시 진행 */ });
    }

    @Test
    void retriesOnceOnOverloadThenSucceeds() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(requestTo(URL)).andRespond(withSuccess(SUCCESS_BODY, MediaType.APPLICATION_JSON));

        String result = generator.generate("hi");

        assertThat(result).isEqualTo("OK");
        server.verify();
    }

    @Test
    void retriesOnceOnGatewayErrorThenSucceeds() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.GATEWAY_TIMEOUT)); // 504
        server.expect(requestTo(URL)).andRespond(withSuccess(SUCCESS_BODY, MediaType.APPLICATION_JSON));

        assertThat(generator.generate("hi")).isEqualTo("OK");
        server.verify();
    }

    @Test
    void retriesOnBadGateway() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.BAD_GATEWAY)); // 502
        server.expect(requestTo(URL)).andRespond(withSuccess(SUCCESS_BODY, MediaType.APPLICATION_JSON));

        assertThat(generator.generate("hi")).isEqualTo("OK");
        server.verify();
    }

    @Test
    void throwsServiceUnavailableWhenOverloadPersists() {
        // 최초 1회 + 재시도 1회 = 총 2회만 호출해야 한다.
        server.expect(ExpectedCount.times(2), requestTo(URL))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(SENSITIVE_ERROR_BODY)
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> generator.generate("hi"))
                .isInstanceOfSatisfying(GeminiUnavailableException.class, e -> {
                    assertThat(e).hasMessage("Gemini response generation failed.");
                    assertThat(e.getCause()).hasMessage("Gemini HTTP 503");
                    assertThat(e.getCause()).hasMessageNotContaining("복약 기록");
                    assertThat(e.getCause().getStackTrace()).isNotEmpty();
                });
        server.verify();
    }

    @Test
    void doesNotRetryOnQuotaExceeded() {
        // 429(쿼터 초과)는 재시도하지 않고 단 1회 호출 후 503으로 안내해야 한다 (비용 절감).
        server.expect(ExpectedCount.once(), requestTo(URL))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> generator.generate("hi"))
                .isInstanceOf(GeminiUnavailableException.class);
        server.verify();
    }

    @Test
    void doesNotRetryNonTransientError() {
        server.expect(ExpectedCount.once(), requestTo(URL))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .body(SENSITIVE_ERROR_BODY)
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> generator.generate("hi"))
                .isInstanceOfSatisfying(GeminiGenerationException.class, e -> {
                    assertThat(e).hasMessage("Gemini response generation failed.");
                    assertThat(e.getCause()).hasMessage("Gemini HTTP 400");
                    assertThat(e.getCause()).hasMessageNotContaining("복약 기록");
                    assertThat(e.getCause().getStackTrace()).isNotEmpty();
                });
        server.verify();
    }
}
