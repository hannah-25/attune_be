package attune.common.filter;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void usesSafeIncomingRequestId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(RequestIdFilter.HEADER_NAME, "client-request-123");
        AtomicReference<String> requestIdInChain = new AtomicReference<>();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                requestIdInChain.set(MDC.get(RequestIdFilter.MDC_KEY)));

        assertThat(requestIdInChain.get()).isEqualTo("client-request-123");
        assertThat(response.getHeader(RequestIdFilter.HEADER_NAME)).isEqualTo("client-request-123");
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void generatesRequestIdWhenIncomingHeaderIsMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> requestIdInChain = new AtomicReference<>();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                requestIdInChain.set(MDC.get(RequestIdFilter.MDC_KEY)));

        assertThat(requestIdInChain.get()).isNotBlank();
        assertThat(response.getHeader(RequestIdFilter.HEADER_NAME)).isEqualTo(requestIdInChain.get());
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void reusesRequestIdOnErrorDispatchReentry() throws ServletException, IOException {
        // 첫 디스패치(REQUEST)
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> {});

        String firstId = response.getHeader(RequestIdFilter.HEADER_NAME);
        assertThat(firstId).isNotBlank();
        // 첫 디스패치 종료 후 MDC는 비워진다.
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();

        // ERROR 디스패치로 재진입(같은 request 객체) — attribute에 저장된 ID를 재사용해야 한다.
        AtomicReference<String> idDuringErrorDispatch = new AtomicReference<>();
        filter.doFilter(request, response, (req, res) ->
                idDuringErrorDispatch.set(MDC.get(RequestIdFilter.MDC_KEY)));

        assertThat(idDuringErrorDispatch.get()).isEqualTo(firstId);
    }

    @Test
    void replacesUnsafeIncomingRequestId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(RequestIdFilter.HEADER_NAME, "bad\nrequest-id");
        AtomicReference<String> requestIdInChain = new AtomicReference<>();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                requestIdInChain.set(MDC.get(RequestIdFilter.MDC_KEY)));

        assertThat(requestIdInChain.get()).isNotEqualTo("bad\nrequest-id");
        assertThat(requestIdInChain.get()).isNotBlank();
        assertThat(response.getHeader(RequestIdFilter.HEADER_NAME)).isEqualTo(requestIdInChain.get());
    }
}
