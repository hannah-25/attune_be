package attune.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    private static final int MAX_REQUEST_ID_LENGTH = 128;
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9._:-]+");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        request.setAttribute(MDC_KEY, requestId);
        response.setHeader(HEADER_NAME, requestId);
        MDC.put(MDC_KEY, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    // ERROR 디스패치(서블릿 /error)에서도 필터를 재실행해 동일 requestId를 MDC에 복원한다.
    // 기본값(true)이면 에러 로그에 requestId가 누락된다.
    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    private String resolveRequestId(HttpServletRequest request) {
        // ERROR 디스패치로 재진입한 경우 첫 디스패치에서 저장한 ID를 재사용한다.
        Object cached = request.getAttribute(MDC_KEY);
        if (cached instanceof String cachedId && StringUtils.hasText(cachedId)) {
            return cachedId;
        }

        String requestId = request.getHeader(HEADER_NAME);
        if (isSafeRequestId(requestId)) {
            return requestId;
        }
        return UUID.randomUUID().toString();
    }

    private boolean isSafeRequestId(String requestId) {
        return StringUtils.hasText(requestId)
                && requestId.length() <= MAX_REQUEST_ID_LENGTH
                && SAFE_REQUEST_ID.matcher(requestId).matches();
    }
}
