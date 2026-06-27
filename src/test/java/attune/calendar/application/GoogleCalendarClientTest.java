package attune.calendar.application;

import attune.calendar.domain.model.CalendarConnection;
import attune.calendar.domain.model.CalendarProvider;
import attune.common.error.InternalServerException;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class GoogleCalendarClientTest {

    private static final String CALENDAR_LIST_URL = "https://www.googleapis.com/calendar/v3/users/me/calendarList";
    private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";
    private static final String SENSITIVE_BODY = """
            {"error":"calendar item: 정신과 진료 일정", "email":"person@example.com"}
            """;

    private MockRestServiceServer server;
    private GoogleCalendarClient client;
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GoogleCalendarClient(builder.build());

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
                .isInstanceOfSatisfying(InternalServerException.class, e -> {
                    assertThat(e).hasMessage("Google Calendar 목록 조회 오류 (500 INTERNAL_SERVER_ERROR)");
                    assertThat(e).hasNoCause();
                    assertThat(e).hasMessageNotContaining("정신과");
                    assertThat(e).hasMessageNotContaining("person@example.com");
                });

        assertThat(logMessages()).allSatisfy(message -> {
            assertThat(message).doesNotContain("정신과");
            assertThat(message).doesNotContain("person@example.com");
        });
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
        server.verify();
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
}
