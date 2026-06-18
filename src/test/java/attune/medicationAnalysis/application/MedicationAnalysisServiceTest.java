package attune.medicationAnalysis.application;

import attune.common.error.BadRequestException;
import attune.medicationAnalysis.application.dto.request.CreateReportRequest;
import attune.medicationAnalysis.application.dto.response.AvailabilityResponse;
import attune.medicationAnalysis.application.dto.response.ReportDetailResponse;
import attune.medicationAnalysis.application.engine.AnalysisEngine;
import attune.medicationAnalysis.application.engine.SnapshotSerializer;
import attune.medicationAnalysis.application.model.AnalysisSnapshot;
import attune.medicationAnalysis.domain.model.MedicationAnalysisReport;
import attune.medicationAnalysis.domain.model.ReportStatus;
import attune.medicationAnalysis.domain.repository.MedicationAnalysisReportRepository;
import attune.medicationAnalysis.infrastructure.GeminiReportClient;
import attune.term.application.TermService;
import attune.user.domain.model.User;
import attune.user.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MedicationAnalysisServiceTest {

    private final MedicationAnalysisReportRepository reportRepository = mock(MedicationAnalysisReportRepository.class);
    private final TermService termService = mock(TermService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final AnalysisEngine analysisEngine = mock(AnalysisEngine.class);
    private final SnapshotSerializer snapshotSerializer = mock(SnapshotSerializer.class);
    private final GeminiReportClient geminiReportClient = mock(GeminiReportClient.class);

    private final MedicationAnalysisService service = new MedicationAnalysisService(
            reportRepository, termService, userRepository,
            analysisEngine, snapshotSerializer, geminiReportClient
    );

    private static final UUID USER_ID = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.now();
    private static final LocalDate START = TODAY.minusDays(13);
    private static final LocalDate END = TODAY.minusDays(1);

    // -------------------------------------------------------------------------
    // checkAvailability
    // -------------------------------------------------------------------------

    @Test
    void checkAvailability_returnsAvailable_whenRecordedDaysAtLeast7() {
        when(analysisEngine.countRecordedDays(USER_ID, START, END)).thenReturn(7);

        AvailabilityResponse response = service.checkAvailability(USER_ID, START, END);

        assertTrue(response.available());
        assertEquals(7, response.recordedDays());
        assertTrue(response.unavailableReasons().isEmpty());
    }

    @Test
    void checkAvailability_returnsUnavailable_whenRecordedDaysBelow7() {
        when(analysisEngine.countRecordedDays(USER_ID, START, END)).thenReturn(5);

        AvailabilityResponse response = service.checkAvailability(USER_ID, START, END);

        assertFalse(response.available());
        assertFalse(response.unavailableReasons().isEmpty());
    }

    // -------------------------------------------------------------------------
    // createReport — period validation
    // -------------------------------------------------------------------------

    @Test
    void createReport_throwsBadRequest_whenEndDateIsFuture() {
        CreateReportRequest request = new CreateReportRequest(TODAY, TODAY.plusDays(1), false);

        assertThrows(BadRequestException.class, () -> service.createReport(USER_ID, request));
    }

    @Test
    void createReport_throwsBadRequest_whenPeriodShorterThan7Days() {
        CreateReportRequest request = new CreateReportRequest(TODAY.minusDays(5), TODAY.minusDays(1), false);

        assertThrows(BadRequestException.class, () -> service.createReport(USER_ID, request));
    }

    @Test
    void createReport_throwsBadRequest_whenPeriodLongerThan90Days() {
        CreateReportRequest request = new CreateReportRequest(TODAY.minusDays(100), TODAY.minusDays(1), false);

        assertThrows(BadRequestException.class, () -> service.createReport(USER_ID, request));
    }

    // -------------------------------------------------------------------------
    // createReport — data quality
    // -------------------------------------------------------------------------

    @Test
    void createReport_throwsBadRequest_whenRecordedDaysInsufficient() {
        when(analysisEngine.countRecordedDays(USER_ID, START, END)).thenReturn(3);
        when(snapshotSerializer.computeHash(USER_ID, START, END)).thenReturn("hash-abc");
        when(reportRepository.findByUser_IdAndPeriodStartAndPeriodEndAndSourceDataHash(any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        CreateReportRequest request = new CreateReportRequest(START, END, false);

        assertThrows(BadRequestException.class, () -> service.createReport(USER_ID, request));
    }

    // -------------------------------------------------------------------------
    // createReport — cache hit
    // -------------------------------------------------------------------------

    @Test
    void createReport_returnsCachedReport_whenHashMatchesCompletedReport() {
        String hash = "same-hash";
        MedicationAnalysisReport cached = completedReport(hash);

        when(analysisEngine.countRecordedDays(USER_ID, START, END)).thenReturn(10);
        when(snapshotSerializer.computeHash(USER_ID, START, END)).thenReturn(hash);
        when(reportRepository.findByUser_IdAndPeriodStartAndPeriodEndAndSourceDataHash(USER_ID, START, END, hash))
                .thenReturn(Optional.of(cached));
        // hash for isOutdated check
        when(snapshotSerializer.computeHash(USER_ID, cached.getPeriodStart(), cached.getPeriodEnd()))
                .thenReturn(hash);

        ReportDetailResponse response = service.createReport(USER_ID, new CreateReportRequest(START, END, false));

        assertEquals(ReportStatus.COMPLETED, response.status());
        verify(analysisEngine, never()).build(any(), any(), any(), anyBoolean());
        verify(geminiReportClient, never()).generate(any(), any());
        verify(reportRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // createReport — new report, AI consent granted
    // -------------------------------------------------------------------------

    @Test
    void createReport_generatesNewReport_withAiResult_whenConsentGranted() {
        String hash = "new-hash";
        String snapshotJson = "{\"period\":{}}";
        String aiJson = "{\"summary\":\"요약\"}";
        AnalysisSnapshot snapshot = minimalSnapshot(START, END);

        when(analysisEngine.countRecordedDays(USER_ID, START, END)).thenReturn(10);
        when(snapshotSerializer.computeHash(USER_ID, START, END)).thenReturn(hash);
        when(reportRepository.findByUser_IdAndPeriodStartAndPeriodEndAndSourceDataHash(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(analysisEngine.build(USER_ID, START, END, false)).thenReturn(snapshot);
        when(snapshotSerializer.toJson(snapshot)).thenReturn(snapshotJson);
        when(termService.isAiAnalysisConsented(USER_ID)).thenReturn(true);
        when(geminiReportClient.generate(eq(snapshotJson), any()))
                .thenReturn(new GeminiReportClient.GeminiReportResult(true, aiJson, "gemini-1.5-flash", "v1.0"));
        when(userRepository.getReferenceById(USER_ID)).thenReturn(mock(User.class));
        when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReportDetailResponse response = service.createReport(USER_ID, new CreateReportRequest(START, END, false));

        assertEquals(ReportStatus.COMPLETED, response.status());
        assertEquals(aiJson, response.aiResultJson());
        verify(geminiReportClient).generate(eq(snapshotJson), any());
    }

    // -------------------------------------------------------------------------
    // createReport — new report, AI consent not granted
    // -------------------------------------------------------------------------

    @Test
    void createReport_generatesNewReport_withoutAiResult_whenConsentNotGranted() {
        String hash = "no-consent-hash";
        String snapshotJson = "{\"period\":{}}";
        AnalysisSnapshot snapshot = minimalSnapshot(START, END);

        when(analysisEngine.countRecordedDays(USER_ID, START, END)).thenReturn(10);
        when(snapshotSerializer.computeHash(USER_ID, START, END)).thenReturn(hash);
        when(reportRepository.findByUser_IdAndPeriodStartAndPeriodEndAndSourceDataHash(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(analysisEngine.build(USER_ID, START, END, false)).thenReturn(snapshot);
        when(snapshotSerializer.toJson(snapshot)).thenReturn(snapshotJson);
        when(termService.isAiAnalysisConsented(USER_ID)).thenReturn(false);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(mock(User.class));
        when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReportDetailResponse response = service.createReport(USER_ID, new CreateReportRequest(START, END, false));

        assertEquals(ReportStatus.COMPLETED, response.status());
        assertNull(response.aiResultJson());
        verify(geminiReportClient, never()).generate(any(), any());
    }

    // -------------------------------------------------------------------------
    // createReport — Gemini failure is handled gracefully
    // -------------------------------------------------------------------------

    @Test
    void createReport_savesReportWithNullAiResult_whenGeminiFails() {
        String hash = "fail-hash";
        String snapshotJson = "{\"period\":{}}";
        AnalysisSnapshot snapshot = minimalSnapshot(START, END);

        when(analysisEngine.countRecordedDays(USER_ID, START, END)).thenReturn(10);
        when(snapshotSerializer.computeHash(USER_ID, START, END)).thenReturn(hash);
        when(reportRepository.findByUser_IdAndPeriodStartAndPeriodEndAndSourceDataHash(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(analysisEngine.build(USER_ID, START, END, false)).thenReturn(snapshot);
        when(snapshotSerializer.toJson(snapshot)).thenReturn(snapshotJson);
        when(termService.isAiAnalysisConsented(USER_ID)).thenReturn(true);
        when(geminiReportClient.generate(eq(snapshotJson), any()))
                .thenReturn(new GeminiReportClient.GeminiReportResult(false, null, "gemini-1.5-flash", "v1.0"));
        when(userRepository.getReferenceById(USER_ID)).thenReturn(mock(User.class));
        when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReportDetailResponse response = service.createReport(USER_ID, new CreateReportRequest(START, END, false));

        assertEquals(ReportStatus.COMPLETED, response.status());
        assertNull(response.aiResultJson());
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private MedicationAnalysisReport completedReport(String hash) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(USER_ID);
        return MedicationAnalysisReport.builder()
                .id(1L)
                .user(user)
                .periodStart(START)
                .periodEnd(END)
                .status(ReportStatus.COMPLETED)
                .sourceDataHash(hash)
                .snapshotJson("{}")
                .aiResultJson("{\"summary\":\"캐시\"}")
                .generatedAt(LocalDateTime.now().minusHours(1))
                .build();
    }

    private AnalysisSnapshot minimalSnapshot(LocalDate start, LocalDate end) {
        return new AnalysisSnapshot(
                new AnalysisSnapshot.Period(start, end, (int) start.until(end.plusDays(1), java.time.temporal.ChronoUnit.DAYS)),
                new AnalysisSnapshot.DataQuality(10, "HIGH", List.of()),
                new AnalysisSnapshot.AdherenceSummary(14, 10, 2, 2, 71.4, 85.7, List.of()),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        );
    }
}
