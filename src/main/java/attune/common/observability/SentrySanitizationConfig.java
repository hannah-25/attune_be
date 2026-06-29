package attune.common.observability;

import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import io.sentry.protocol.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class SentrySanitizationConfig {

    private static final String REQUEST_ID_HEADER = "x-request-id";

    @Bean
    public SentryOptions.BeforeSendCallback sentryPiiSanitizer() {
        return (event, hint) -> sanitize(event);
    }

    SentryEvent sanitize(SentryEvent event) {
        if (event == null) {
            return null;
        }
        event.setUser(null);
        sanitizeRequest(event.getRequest());
        return event;
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
        Map<String, String> safe = new LinkedHashMap<>();
        if (headers != null) {
            headers.forEach((name, value) -> {
                if (REQUEST_ID_HEADER.equalsIgnoreCase(name)) {
                    safe.put(name, value);
                }
            });
        }
        return safe;
    }
}
