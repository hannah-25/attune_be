package attune.common.observability;

import io.sentry.Breadcrumb;
import io.sentry.SentryBaseEvent;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import io.sentry.protocol.Request;
import io.sentry.protocol.SentryTransaction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "sentry.enabled", havingValue = "true")
public class SentrySanitizationConfig {

    private static final String REQUEST_ID_HEADER = "x-request-id";
    private static final String REQUEST_ID_HEADER_CANONICAL = "X-Request-Id";
    private static final String HTTP_BREADCRUMB_CATEGORY = "http";

    @Bean
    public SentryOptions.BeforeSendCallback sentryPiiSanitizer() {
        return (event, hint) -> sanitize(event);
    }

    @Bean
    public SentryOptions.BeforeSendTransactionCallback sentryTransactionPiiSanitizer() {
        return (transaction, hint) -> sanitizeTransaction(transaction);
    }

    @Bean
    public SentryOptions.BeforeBreadcrumbCallback sentryBreadcrumbSanitizer() {
        return (breadcrumb, hint) -> sanitizeBreadcrumb(breadcrumb);
    }

    SentryEvent sanitize(SentryEvent event) {
        if (event == null) {
            return null;
        }
        sanitizeBaseEvent(event);
        return event;
    }

    SentryTransaction sanitizeTransaction(SentryTransaction transaction) {
        if (transaction == null) {
            return null;
        }
        sanitizeBaseEvent(transaction);
        return transaction;
    }

    Breadcrumb sanitizeBreadcrumb(Breadcrumb breadcrumb) {
        if (breadcrumb == null) {
            return null;
        }
        // HTTP breadcrumb는 BeforeSendCallback을 우회하므로 query/fragment를 제거한다.
        // 키가 없으면 no-op이라 안전하다.
        if (HTTP_BREADCRUMB_CATEGORY.equals(breadcrumb.getCategory())) {
            breadcrumb.removeData("http.query");
            breadcrumb.removeData("http.fragment");
        }
        return breadcrumb;
    }

    private void sanitizeBaseEvent(SentryBaseEvent event) {
        event.setUser(null);
        sanitizeRequest(event.getRequest());
    }

    private void sanitizeRequest(Request request) {
        if (request == null) {
            return;
        }
        request.setCookies(null);
        request.setData(null);
        request.setQueryString(null);
        request.setHeaders(safeHeaders(request.getHeaders()));
    }

    private Map<String, String> safeHeaders(Map<String, String> headers) {
        if (headers == null) {
            return null;
        }
        Map<String, String> safe = new LinkedHashMap<>();
        headers.forEach((name, value) -> {
            if (REQUEST_ID_HEADER.equalsIgnoreCase(name) && value != null) {
                safe.put(REQUEST_ID_HEADER_CANONICAL, value);
            }
        });
        return safe;
    }
}
