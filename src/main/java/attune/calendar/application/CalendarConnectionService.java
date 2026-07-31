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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
        LocalDateTime now = LocalDateTime.now();

        CalendarConnection connection = calendarConnectionRepository
                .findByUserIdAndProvider(userId, CalendarProvider.GOOGLE)
                .orElse(null);

        if (connection == null && (token.refreshToken() == null || token.refreshToken().isBlank())) {
            throw new BadRequestException("Google Calendar 연동을 위해 오프라인 액세스 권한이 필요합니다. access_type=offline으로 재인증해 주세요.");
        }

        if (connection == null) {
            connection = CalendarConnection.google(
                    userId,
                    accountEmail,
                    token.accessToken(),
                    token.refreshToken(),
                    token.expiresAt(),
                    now
            );
            connection = calendarConnectionRepository.save(connection);
        } else {
            connection.reconnect(accountEmail, token.accessToken(), token.refreshToken(), token.expiresAt(), now);
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

        LocalDateTime now = LocalDateTime.now();
        refreshTokenIfNeeded(connection, now);

        List<String> calendarIds = googleCalendarClient.listCalendarIds(connection);
        connection.updateSelectedCalendarIds(calendarIds, now);

        LocalDate today = LocalDate.now();
        LocalDateTime startAt = today.minusMonths(1).atStartOfDay();
        LocalDateTime endAt = today.plusMonths(3).plusDays(1).atStartOfDay();
        LocalDateTime syncedAt = now;

        // Google에서 전체 스냅샷 수집
        List<ExternalCalendarEventSnapshot> allSnapshots = new ArrayList<>();
        for (String calendarId : calendarIds) {
            allSnapshots.addAll(googleCalendarClient.listEvents(connection, calendarId, startAt, endAt));
        }

        // DB에서 해당 범위 기존 이벤트를 한 번에 조회 → Map 인덱싱
        Map<String, ExternalCalendarEvent> existingByKey = externalCalendarEventRepository
                .findAllByConnectionIdInRange(connection.getId(), startAt, endAt)
                .stream()
                .collect(Collectors.toMap(
                        e -> e.getProviderCalendarId() + "|" + e.getProviderEventId(),
                        e -> e,
                        (existing, replacement) -> existing
                ));

        // 메모리에서 upsert 판단, 신규만 배치 저장
        List<ExternalCalendarEvent> toCreate = new ArrayList<>();
        int syncedCount = 0;

        for (ExternalCalendarEventSnapshot snapshot : allSnapshots) {
            String key = snapshot.providerCalendarId() + "|" + snapshot.providerEventId();
            ExternalCalendarEvent existing = existingByKey.get(key);

            if (existing == null) {
                if (!snapshot.deleted()) {
                    toCreate.add(ExternalCalendarEvent.create(connection, snapshot, syncedAt));
                    syncedCount++;
                }
            } else {
                existing.updateFrom(snapshot, syncedAt);
                syncedCount++;
            }
        }

        externalCalendarEventRepository.saveAll(toCreate);

        connection.markSynced(syncedAt);
        return new CalendarSyncResponse(connection.getId(), syncedAt, syncedCount);
    }

    @Transactional
    public void disconnect(Long connectionId) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        CalendarConnection connection = calendarConnectionRepository.findByIdAndUserIdAndIsActiveTrue(connectionId, userId)
                .orElseThrow(() -> new NotFoundException("Calendar connection not found"));
        connection.deactivate(LocalDateTime.now());
    }

    private void refreshTokenIfNeeded(CalendarConnection connection, LocalDateTime now) {
        if (connection.getTokenExpiresAt() != null && connection.getTokenExpiresAt().isAfter(now.plusMinutes(5))) {
            return;
        }
        if (connection.getRefreshToken() == null || connection.getRefreshToken().isBlank()) {
            throw new BadRequestException("Google Calendar refresh token is missing. Please reconnect Google Calendar.");
        }

        GoogleCalendarClient.GoogleToken token = googleCalendarClient.refresh(connection);
        connection.updateAccessToken(token.accessToken(), token.expiresAt(), now);
    }
}
