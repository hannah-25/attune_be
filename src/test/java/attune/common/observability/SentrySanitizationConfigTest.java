package attune.common.observability;

import io.sentry.Breadcrumb;
import io.sentry.SentryEvent;
import io.sentry.protocol.Request;
import io.sentry.protocol.SentryTransaction;
import io.sentry.protocol.TransactionInfo;
import io.sentry.protocol.User;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SentrySanitizationConfigTest {

    private final SentrySanitizationConfig config = new SentrySanitizationConfig();

    @Test
    void removesUserRequestBodyCookiesQueryAndSensitiveHeaders() {
        SentryEvent event = new SentryEvent();
        event.setUser(new User());

        Request request = new Request();
        request.setCookies("SESSION=secret");
        request.setData("{\"symptom\":\"sensitive\"}");
        request.setQueryString("token=secret");
        request.setHeaders(headers());
        event.setRequest(request);

        SentryEvent sanitized = config.sanitize(event);

        assertThat(sanitized.getUser()).isNull();
        assertThat(sanitized.getRequest().getCookies()).isNull();
        assertThat(sanitized.getRequest().getData()).isNull();
        assertThat(sanitized.getRequest().getQueryString()).isNull();
        assertThat(sanitized.getRequest().getHeaders())
                .containsOnly(Map.entry("X-Request-Id", "request-1"));
    }

    @Test
    void sanitizesTransactionUserAndRequest() {
        SentryTransaction transaction = new SentryTransaction(
                "test-tx", null, null, List.of(), Map.of(), new TransactionInfo("manual"));
        transaction.setUser(new User());

        Request request = new Request();
        request.setCookies("SESSION=secret");
        request.setData("{\"symptom\":\"sensitive\"}");
        request.setQueryString("token=secret");
        request.setHeaders(headers());
        transaction.setRequest(request);

        SentryTransaction sanitized = config.sanitizeTransaction(transaction);

        assertThat(sanitized.getUser()).isNull();
        assertThat(sanitized.getRequest().getCookies()).isNull();
        assertThat(sanitized.getRequest().getData()).isNull();
        assertThat(sanitized.getRequest().getQueryString()).isNull();
        assertThat(sanitized.getRequest().getHeaders())
                .containsOnly(Map.entry("X-Request-Id", "request-1"));
    }

    @Test
    void stripsQueryAndFragmentFromHttpBreadcrumb() {
        Breadcrumb breadcrumb = new Breadcrumb();
        breadcrumb.setCategory("http");
        breadcrumb.setData("url", "https://api.example.com/v1/users");
        breadcrumb.setData("http.query", "token=secret");
        breadcrumb.setData("http.fragment", "section");

        Breadcrumb sanitized = config.sanitizeBreadcrumb(breadcrumb);

        assertThat(sanitized.getData("http.query")).isNull();
        assertThat(sanitized.getData("http.fragment")).isNull();
        assertThat(sanitized.getData("url")).isEqualTo("https://api.example.com/v1/users");
    }

    @Test
    void keepsNonHttpBreadcrumbUntouched() {
        Breadcrumb breadcrumb = new Breadcrumb();
        breadcrumb.setCategory("auth");
        breadcrumb.setData("http.query", "kept-because-not-http");

        Breadcrumb sanitized = config.sanitizeBreadcrumb(breadcrumb);

        assertThat(sanitized.getData("http.query")).isEqualTo("kept-because-not-http");
    }

    private Map<String, String> headers() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer secret");
        headers.put("Cookie", "SESSION=secret");
        headers.put("X-Request-Id", "request-1");
        return headers;
    }
}
