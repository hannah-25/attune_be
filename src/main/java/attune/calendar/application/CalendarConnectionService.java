package attune.calendar.application;

import attune.calendar.application.dto.request.ConnectGoogleCalendarRequest;
import attune.calendar.application.dto.response.CalendarConnectionListResponse;
import attune.calendar.application.dto.response.CalendarConnectionResponse;
import attune.calendar.application.dto.response.CalendarSyncResponse;
import attune.calendar.domain.model.CalendarConnection;
import attune.calendar.domain.model.CalendarProvider;
import attune.calendar.domain.model.ExternalCalendarEvent;
import attune.calendar.domain.model.ExternalCalendarEventSnapshot;
import attune.calendar.domain.repository.CalendarConnectionRepository;
import attune.calendar.domain.repository.ExternalCalendarEventRepository;
import attune.common.error.BadRequestException;
import attune.common.error.NotFoundException;
import attune.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CalendarConnectionService {

    private final CalendarConnectionRepository calendarConnectionRepository;
    private final ExternalCalendarEventRepository externalCalendarEventRepository;
    private final GoogleCalendarClient googleCalendarClient;

    @Transactional(readOnly = true)
    public CalendarConnectionListResponse getConnections() {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        List<CalendarConnectionResponse> connections = calendarConnectionRepository
                .findAllByUserIdAndIsActiveTrue(userId)
                .stream()
                .map(CalendarConnectionResponse::from)
                .toList();
        return new CalendarConnectionListResponse(connections);
    }

    @Transactional
    public CalendarConnectionResponse connectGoogle(ConnectGoogleCalendarRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        GoogleCalendarClient.GoogleToken token = googleCalendarClient.exchangeCode(
                request.authorizationCode(),
                request.redirectUri()
        );
        String accountEmail = googleCalendarClient.fetchAccountEmail(token.accessToken());

        CalendarConnection connection = calendarConnectionRepository
                .findByUserIdAndProviderAndIsActiveTrue(userId, CalendarProvider.GOOGLE)
                .orElse(null);

        if (connection == null) {
            connection = CalendarConnection.google(
                    userId,
                    accountEmail,
                    token.accessToken(),
                    token.refreshToken(),
                    token.expiresAt()
            );
            connection = calendarConnectionRepository.save(connection);
        } else {
            connection.reconnect(accountEmail, token.accessToken(), token.refreshToken(), token.expiresAt());
        }

        return CalendarConnectionResponse.from(connection);
    }

    @Transactional
    public CalendarSyncResponse sync(Long connectionId) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        CalendarConnection connection = calendarConnectionRepository.findByIdAndUserIdAndIsActiveTrue(connectionId, userId)
                .orElseThrow(() -> new NotFoundException("Calendar connection not found"));

        if (connection.getProvider() != CalendarProvider.GOOGLE) {
            throw new BadRequestException("Unsupported calendar provider");
        }

        refreshTokenIfNeeded(connection);

        List<String> calendarIds = googleCalendarClient.listCalendarIds(connection);
        connection.updateSelectedCalendarIds(calendarIds);

        LocalDateTime startAt = LocalDate.now().minusMonths(1).atStartOfDay();
        LocalDateTime endAt = LocalDate.now().plusMonths(3).plusDays(1).atStartOfDay();
        LocalDateTime syncedAt = LocalDateTime.now();

        int syncedCount = 0;
        for (String calendarId : calendarIds) {
            List<ExternalCalendarEventSnapshot> snapshots = googleCalendarClient.listEvents(connection, calendarId, startAt, endAt);
            for (ExternalCalendarEventSnapshot snapshot : snapshots) {
                if (upsertExternalEvent(connection, snapshot, syncedAt)) {
                    syncedCount++;
                }
            }
        }

        connection.markSynced(syncedAt);
        return new CalendarSyncResponse(connection.getId(), syncedAt, syncedCount);
    }

    @Transactional
    public void disconnect(Long connectionId) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        CalendarConnection connection = calendarConnectionRepository.findByIdAndUserIdAndIsActiveTrue(connectionId, userId)
                .orElseThrow(() -> new NotFoundException("Calendar connection not found"));
        connection.deactivate();
    }

    private void refreshTokenIfNeeded(CalendarConnection connection) {
        if (connection.getTokenExpiresAt() != null && connection.getTokenExpiresAt().isAfter(LocalDateTime.now().plusMinutes(5))) {
            return;
        }
        if (connection.getRefreshToken() == null || connection.getRefreshToken().isBlank()) {
            throw new BadRequestException("Google Calendar refresh token is missing. Please reconnect Google Calendar.");
        }

        GoogleCalendarClient.GoogleToken token = googleCalendarClient.refresh(connection);
        connection.updateAccessToken(token.accessToken(), token.expiresAt());
    }

    private boolean upsertExternalEvent(CalendarConnection connection, ExternalCalendarEventSnapshot snapshot, LocalDateTime syncedAt) {
        ExternalCalendarEvent event = externalCalendarEventRepository
                .findByConnectionIdAndProviderCalendarIdAndProviderEventId(
                        connection.getId(),
                        snapshot.providerCalendarId(),
                        snapshot.providerEventId()
                )
                .orElse(null);

        if (event == null) {
            if (snapshot.deleted()) {
                return false;
            }
            externalCalendarEventRepository.save(ExternalCalendarEvent.create(connection, snapshot, syncedAt));
            return true;
        }

        event.updateFrom(snapshot, syncedAt);
        return true;
    }
}
