package attune.ai.adapter.gemini;

import attune.ai.config.GeminiProperties;
import attune.common.error.internalserver.GeminiGenerationException;
import attune.common.error.serviceunavailable.GeminiUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiTextGeneratorTest {

    private static final String BASE_URL = "http://gemini.test";
    private static final String URL =
            BASE_URL + "/v1beta/models/gemini-2.5-flash:generateContent";
    private static final String SUCCESS_BODY =
            "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"OK\"}]}}]}";

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
    void throwsServiceUnavailableWhenOverloadPersists() {
        // 최초 1회 + 재시도 1회 = 총 2회만 호출해야 한다.
        server.expect(ExpectedCount.times(2), requestTo(URL))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> generator.generate("hi"))
                .isInstanceOf(GeminiUnavailableException.class);
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
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> generator.generate("hi"))
                .isInstanceOf(GeminiGenerationException.class);
        server.verify();
    }
}
