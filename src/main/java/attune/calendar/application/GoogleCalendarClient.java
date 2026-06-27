package attune.calendar.application;

import attune.calendar.domain.model.CalendarConnection;
import attune.calendar.domain.model.ExternalCalendarEventSnapshot;
import attune.common.error.InternalServerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleCalendarClient {

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";
    private static final String CALENDAR_LIST_URL = "https://www.googleapis.com/calendar/v3/users/me/calendarList";
    private static final String EVENTS_URL = "https://www.googleapis.com/calendar/v3/calendars/{calendarId}/events";

    private final RestClient oauthRestClient;

    @Value("${oauth.google.calendar-client-id:${GOOGLE_CALENDAR_CLIENT_ID:}}")
    private String calendarClientId;

    @Value("${oauth.google.client-secret:}")
    private String calendarClientSecret;

    @Value("${oauth.google.client-ids:${GOOGLE_CLIENT_IDS:}}")
    private String loginClientIds;

    public GoogleToken exchangeCode(String authorizationCode, String redirectUri) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", authorizationCode);
        body.add("redirect_uri", redirectUri);
        body.add("client_id", resolveClientId());
        body.add("client_secret", resolveClientSecret());

        Map<String, Object> response = postToken(body);
        return toToken(response);
    }

    public GoogleToken refresh(CalendarConnection connection) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", connection.getRefreshToken());
        body.add("client_id", resolveClientId());
        body.add("client_secret", resolveClientSecret());

        Map<String, Object> response = postToken(body);
        return toToken(response);
    }

    public String fetchAccountEmail(String accessToken) {
        try {
            Map<String, Object> response = oauthRestClient.get()
                    .uri(USERINFO_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            Object email = response == null ? null : response.get("email");
            return email instanceof String value && !value.isBlank() ? value : null;
        } catch (RestClientResponseException e) {
            log.warn("Failed to fetch Google account email: status={}", e.getStatusCode());
            return null;
        } catch (RuntimeException e) {
            log.warn("Failed to fetch Google account email: type={}", e.getClass().getSimpleName());
            return null;
        }
    }

    public List<String> listCalendarIds(CalendarConnection connection) {
        Map<String, Object> response;
        try {
            response = oauthRestClient.get()
                    .uri(CALENDAR_LIST_URL)
                    .header("Authorization", "Bearer " + connection.getAccessToken())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientResponseException e) {
            throw calendarApiException("Google CalendarList API error", "Google Calendar 목록 조회 오류", e);
        }

        Object items = response == null ? null : response.get("items");
        if (!(items instanceof List<?> list)) {
            log.warn("Google CalendarList: items missing or not a list");
            return List.of("primary");
        }

        List<String> calendarIds = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> cal) {
                String id = string(cal.get("id"));
                if (id != null && !id.isBlank()) {
                    calendarIds.add(id);
                }
            }
        }
        log.debug("Google CalendarList: {} calendars found", calendarIds.size());
        return calendarIds.isEmpty() ? List.of("primary") : calendarIds;
    }

    public List<ExternalCalendarEventSnapshot> listEvents(CalendarConnection connection,
                                                          String calendarId,
                                                          LocalDateTime startAt,
                                                          LocalDateTime endAt) {
        List<ExternalCalendarEventSnapshot> snapshots = new ArrayList<>();
        String pageToken = null;

        do {
            UriComponentsBuilder uriBuilder = UriComponentsBuilder
                    .fromUriString(EVENTS_URL)
                    .queryParam("timeMin", toGoogleDateTime(startAt))
                    .queryParam("timeMax", toGoogleDateTime(endAt))
                    .queryParam("singleEvents", "true")
                    .queryParam("showDeleted", "true")
                    .queryParam("orderBy", "startTime");

            if (pageToken != null) {
                uriBuilder.queryParam("pageToken", pageToken);
            }

            String uri = uriBuilder.build().toUriString();
            log.debug("Google Calendar listEvents URI: {}", uri);

            Map<String, Object> response;
            try {
                response = oauthRestClient.get()
                        .uri(uri, calendarId)
                        .header("Authorization", "Bearer " + connection.getAccessToken())
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {});
            } catch (RestClientResponseException e) {
                throw calendarApiException("Google Calendar API error", "Google Calendar API 오류", e);
            }

            if (response == null) {
                break;
            }

            Object items = response.get("items");
            if (items instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> event) {
                        toSnapshot(calendarId, event).ifPresent(snapshots::add);
                    }
                }
                log.debug("Google Calendar listEvents: page {} items, calendarId={}", list.size(), calendarId);
            }

            pageToken = response.get("nextPageToken") instanceof String token ? token : null;
        } while (pageToken != null);

        log.debug("Google Calendar listEvents: {} total snapshots for calendarId={}", snapshots.size(), calendarId);
        return snapshots;
    }

    private Map<String, Object> postToken(MultiValueMap<String, String> body) {
        try {
            return oauthRestClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientResponseException e) {
            throw calendarApiException("Google Calendar token API error", "Google Calendar token exchange failed", e);
        } catch (RuntimeException e) {
            throw new InternalServerException("Google Calendar token exchange failed", e);
        }
    }

    private GoogleToken toToken(Map<String, Object> response) {
        if (response == null || !(response.get("access_token") instanceof String accessToken)) {
            throw new InternalServerException("Google Calendar token response did not include an access token");
        }

        String refreshToken = response.get("refresh_token") instanceof String value ? value : null;
        long expiresIn = response.get("expires_in") instanceof Number value ? value.longValue() : 3600L;
        return new GoogleToken(accessToken, refreshToken, LocalDateTime.now().plusSeconds(expiresIn));
    }

    private java.util.Optional<ExternalCalendarEventSnapshot> toSnapshot(String calendarId, Map<?, ?> event) {
        String eventId = string(event.get("id"));
        if (eventId == null) {
            return java.util.Optional.empty();
        }

        boolean deleted = "cancelled".equals(string(event.get("status")));
        DateRange range = readDateRange(event);
        if (range == null) {
            return java.util.Optional.empty();
        }

        String title = string(event.get("summary"));
        if (title == null || title.isBlank()) {
            title = "제목 없음";
        }

        return java.util.Optional.of(new ExternalCalendarEventSnapshot(
                calendarId,
                eventId,
                title,
                string(event.get("description")),
                string(event.get("location")),
                range.allDay(),
                range.startTime(),
                range.endTime(),
                parseDateTime(string(event.get("updated"))),
                deleted,
                string(event.get("etag"))
        ));
    }

    private DateRange readDateRange(Map<?, ?> event) {
        Object start = event.get("start");
        Object end = event.get("end");
        if (!(start instanceof Map<?, ?> startMap) || !(end instanceof Map<?, ?> endMap)) {
            return null;
        }

        String startDateTime = string(startMap.get("dateTime"));
        String endDateTime = string(endMap.get("dateTime"));
        if (startDateTime != null && endDateTime != null) {
            return new DateRange(parseDateTime(startDateTime), parseDateTime(endDateTime), false);
        }

        String startDate = string(startMap.get("date"));
        String endDate = string(endMap.get("date"));
        if (startDate != null && endDate != null) {
            return new DateRange(
                    LocalDate.parse(startDate).atStartOfDay(),
                    LocalDate.parse(endDate).atStartOfDay(),
                    true
            );
        }

        return null;
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }

    private String toGoogleDateTime(LocalDateTime value) {
        return value.atZone(ZoneId.systemDefault()).toInstant().toString();
    }

    private String resolveClientId() {
        if (calendarClientId != null && !calendarClientId.isBlank()) {
            return calendarClientId.trim();
        }
        if (loginClientIds != null && !loginClientIds.isBlank()) {
            String first = Arrays.stream(loginClientIds.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .findFirst()
                    .orElse(null);
            if (first != null) {
                return first;
            }
        }
        throw new InternalServerException("Google Calendar client id is not configured");
    }

    private String resolveClientSecret() {
        if (calendarClientSecret != null && !calendarClientSecret.isBlank()) {
            return calendarClientSecret.trim();
        }
        throw new InternalServerException("Google Calendar client secret is not configured");
    }

    private String string(Object value) {
        return value instanceof String text ? text : null;
    }

    private InternalServerException calendarApiException(String logPrefix,
                                                        String clientMessage,
                                                        RestClientResponseException e) {
        // PII 금지: Google 응답 body는 일정 제목/설명, 계정 정보, 토큰 오류 세부정보를 포함할 수 있다.
        // RestClientResponseException 자체도 body를 들고 있으므로 cause로 보존하지 않는다.
        log.error("{}: status={}", logPrefix, e.getStatusCode());
        return new InternalServerException(clientMessage + " (" + e.getStatusCode() + ")");
    }

    public record GoogleToken(String accessToken, String refreshToken, LocalDateTime expiresAt) {
    }

    private record DateRange(LocalDateTime startTime, LocalDateTime endTime, boolean allDay) {
    }
}
