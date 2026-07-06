package attune.calendar.application;

import attune.calendar.domain.model.CalendarConnection;
import attune.calendar.domain.model.CalendarProvider;
import attune.common.error.InternalServerException;
import attune.common.error.badrequest.CalendarReauthRequiredException;
import attune.common.error.serviceunavailable.GoogleCalendarUnavailableException;
import attune.common.observability.ObservabilityMetrics;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class GoogleCalendarClientTest {

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String CALENDAR_LIST_URL = "https://www.googleapis.com/calendar/v3/users/me/calendarList";
    private static final String EVENTS_URL_PREFIX = "https://www.googleapis.com/calendar/v3/calendars/primary/events";
    private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";
    private static final String SENSITIVE_BODY = """
            {"error":"calendar item: 정신과 진료 일정", "email":"person@example.com"}
            """;

    private MockRestServiceServer server;
    private GoogleCalendarClient client;
    private SimpleMeterRegistry meterRegistry;
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        meterRegistry = new SimpleMeterRegistry();
        client = new GoogleCalendarClient(builder.build(), new ObservabilityMetrics(meterRegistry));

        logger = (Logger) LoggerFactory.getLogger(GoogleCalendarClient.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    void listCalendarIds_doesNotExposeGoogleResponseBodyInExceptionOrLog() {
        server.expect(requestTo(CALENDAR_LIST_URL))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(SENSITIVE_BODY));

        assertThatThrownBy(() -> client.listCalendarIds(connection()))
                .isInstanceOfSatisfying(GoogleCalendarUnavailableException.class, e -> {
                    assertThat(e).hasMessage("Google Calendar 목록 조회 오류 (500 INTERNAL_SERVER_ERROR)");
                    assertThat(e).hasNoCause();
                    assertThat(e).hasMessageNotContaining("정신과");
                    assertThat(e).hasMessageNotContaining("person@example.com");
                });

        assertThat(logMessages()).allSatisfy(message -> {
            assertThat(message).doesNotContain("정신과");
            assertThat(message).doesNotContain("person@example.com");
        });
        assertThat(logThrowableMessages()).contains("Google API Error: 500 INTERNAL_SERVER_ERROR");
        assertThat(logThrowableMessages()).allSatisfy(message -> {
            assertThat(message).doesNotContain("정신과");
            assertThat(message).doesNotContain("person@example.com");
        });
        assertThat(calendarRequestCount("calendar_list", "unavailable")).isEqualTo(1.0);
        server.verify();
    }

    @Test
    void listCalendarIds_expiredTokenIsReportedAsReauthRequired() {
        server.expect(requestTo(CALENDAR_LIST_URL))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(SENSITIVE_BODY));

        assertThatThrownBy(() -> client.listCalendarIds(connection()))
                .isInstanceOfSatisfying(CalendarReauthRequiredException.class, e -> {
                    assertThat(e).hasMessage("Google Calendar 연동이 만료되었습니다. 다시 연결해 주세요.");
                    assertThat(e).hasNoCause();
                });

        assertThat(logMessages()).allSatisfy(message -> {
            assertThat(message).doesNotContain("정신과");
            assertThat(message).doesNotContain("person@example.com");
        });
        assertThat(calendarRequestCount("calendar_list", "reauth")).isEqualTo(1.0);
        server.verify();
    }

    @Test
    void listCalendarIds_rateLimitIsReportedAsUnavailable() {
        server.expect(requestTo(CALENDAR_LIST_URL))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(SENSITIVE_BODY));

        assertThatThrownBy(() -> client.listCalendarIds(connection()))
                .isInstanceOf(GoogleCalendarUnavailableException.class)
                .hasNoCause();

        assertThat(calendarRequestCount("calendar_list", "unavailable")).isEqualTo(1.0);
        server.verify();
    }

    @Test
    void listCalendarIds_unexpected4xxRemainsInternalServerError() {
        server.expect(requestTo(CALENDAR_LIST_URL))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(SENSITIVE_BODY));

        assertThatThrownBy(() -> client.listCalendarIds(connection()))
                .isInstanceOfSatisfying(InternalServerException.class, e -> {
                    assertThat(e).hasMessage("Google Calendar 목록 조회 오류 (404 NOT_FOUND)");
                    assertThat(e).hasNoCause();
                });

        assertThat(calendarRequestCount("calendar_list", "failure")).isEqualTo(1.0);
        server.verify();
    }

    @Test
    void listEvents_serverErrorIsReportedAsUnavailable() {
        server.expect(requestTo(startsWith(EVENTS_URL_PREFIX)))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(SENSITIVE_BODY));

        assertThatThrownBy(() -> client.listEvents(
                connection(), "primary",
                LocalDateTime.now().minusDays(7), LocalDateTime.now()))
                .isInstanceOfSatisfying(GoogleCalendarUnavailableException.class, e -> {
                    assertThat(e).hasMessage("Google Calendar API 오류 (503 SERVICE_UNAVAILABLE)");
                    assertThat(e).hasNoCause();
                    assertThat(e).hasMessageNotContaining("정신과");
                });

        assertThat(calendarRequestCount("events", "unavailable")).isEqualTo(1.0);
        server.verify();
    }

    @Test
    void refresh_revokedTokenIsReportedAsReauthRequired() {
        ReflectionTestUtils.setField(client, "calendarClientId", "client-id");
        ReflectionTestUtils.setField(client, "calendarClientSecret", "client-secret");
        server.expect(requestTo(TOKEN_URL))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"error":"invalid_grant","error_description":"Token has been expired or revoked."}
                                """));

        assertThatThrownBy(() -> client.refresh(connection()))
                .isInstanceOfSatisfying(CalendarReauthRequiredException.class, e -> {
                    assertThat(e).hasNoCause();
                    assertThat(e).hasMessageNotContaining("invalid_grant");
                });

        assertThat(logMessages()).allSatisfy(message ->
                assertThat(message).doesNotContain("invalid_grant"));
        assertThat(logThrowableMessages()).allSatisfy(message ->
                assertThat(message).doesNotContain("invalid_grant"));
        assertThat(calendarRequestCount("token", "reauth")).isEqualTo(1.0);
        server.verify();
    }

    @Test
    void fetchAccountEmail_doesNotExposeGoogleResponseBodyInLog() {
        server.expect(requestTo(USERINFO_URL))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(SENSITIVE_BODY));

        String result = client.fetchAccountEmail("access-token");

        assertThat(result).isNull();
        assertThat(logMessages()).allSatisfy(message -> {
            assertThat(message).doesNotContain("정신과");
            assertThat(message).doesNotContain("person@example.com");
        });
        assertThat(logThrowableMessages()).contains("Google account email API error: 401 UNAUTHORIZED");
        assertThat(logThrowableMessages()).allSatisfy(message -> {
            assertThat(message).doesNotContain("정신과");
            assertThat(message).doesNotContain("person@example.com");
        });
        assertThat(calendarRequestCount("userinfo", "failure")).isEqualTo(1.0);
        server.verify();
    }

    @Test
    void fetchAccountEmail_connectionErrorIsRecordedAsUnavailable() {
        server.expect(requestTo(USERINFO_URL))
                .andRespond(withException(new SocketTimeoutException("connect timed out")));

        String result = client.fetchAccountEmail("access-token");

        assertThat(result).isNull();
        assertThat(logThrowableMessages()).contains("Google account email API connection error");
        assertThat(logThrowableMessages()).allSatisfy(message ->
                assertThat(message).doesNotContain(USERINFO_URL));
        assertThat(calendarRequestCount("userinfo", "unavailable")).isEqualTo(1.0);
        server.verify();
    }

    private double calendarRequestCount(String operation, String outcome) {
        return meterRegistry.counter("attune.calendar.requests",
                "operation", operation,
                "outcome", outcome
        ).count();
    }

    private CalendarConnection connection() {
        return CalendarConnection.builder()
                .userId(UUID.randomUUID())
                .provider(CalendarProvider.GOOGLE)
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .tokenExpiresAt(LocalDateTime.now().plusHours(1))
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private java.util.List<String> logMessages() {
        return appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }

    private java.util.List<String> logThrowableMessages() {
        return appender.list.stream()
                .map(ILoggingEvent::getThrowableProxy)
                .filter(java.util.Objects::nonNull)
                .map(IThrowableProxy::getMessage)
                .toList();
    }
}
