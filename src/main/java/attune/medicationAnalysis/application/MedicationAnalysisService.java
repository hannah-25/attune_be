package attune.medicationAnalysis.application;

import attune.common.error.BadRequestException;
import attune.common.error.NotFoundException;
import attune.medicationAnalysis.application.dto.request.CreateReportRequest;
import attune.medicationAnalysis.application.dto.response.AdherenceSummaryResponse;
import attune.medicationAnalysis.application.dto.response.AvailabilityResponse;
import attune.medicationAnalysis.application.dto.response.ReportDetailResponse;
import attune.medicationAnalysis.application.dto.response.ReportListItemResponse;
import attune.medicationAnalysis.application.engine.AnalysisEngine;
import attune.medicationAnalysis.application.engine.AnalysisRawData;
import attune.medicationAnalysis.application.engine.SnapshotSerializer;
import attune.medicationAnalysis.application.model.AnalysisSnapshot;
import attune.medicationAnalysis.domain.model.MedicationAnalysisReport;
import attune.medicationAnalysis.domain.model.ReportStatus;
import attune.medicationAnalysis.domain.repository.MedicationAnalysisReportRepository;
import attune.medicationAnalysis.infrastructure.GeminiReportClient;
import attune.term.application.TermService;
import attune.user.domain.model.User;
import attune.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MedicationAnalysisService {

    private static final int MIN_PERIOD_DAYS = 7;
    private static final int MAX_PERIOD_DAYS = 90;
    private static final int MIN_RECORDED_DAYS = 7;

    private final MedicationAnalysisReportRepository reportRepository;
    private final TermService termService;
    private final UserRepository userRepository;
    private final AnalysisEngine analysisEngine;
    private final SnapshotSerializer snapshotSerializer;
    private final GeminiReportClient geminiReportClient;
    private final Clock clock;

    @Transactional(readOnly = true)
    public AvailabilityResponse checkAvailability(UUID userId, LocalDate startDate, LocalDate endDate) {
        validatePeriod(startDate, endDate);
        int recordedDays = analysisEngine.countRecordedDays(userId, startDate, endDate);
        boolean available = recordedDays >= MIN_RECORDED_DAYS;
        List<String> reasons = available ? List.of()
                : List.of("일지 기록일이 " + MIN_RECORDED_DAYS + "일 미만입니다. (" + recordedDays + "일 기록됨)");
        return new AvailabilityResponse(available, recordedDays, reasons);
    }

    @Transactional(readOnly = true)
    public AdherenceSummaryResponse getSummary(UUID userId, LocalDate startDate, LocalDate endDate) {
        validatePeriod(startDate, endDate);
        AnalysisSnapshot snapshot = analysisEngine.build(userId, startDate, endDate, false);
        AnalysisSnapshot.AdherenceSummary s = snapshot.adherenceSummary();
        return new AdherenceSummaryResponse(
                s.totalScheduled(), s.takenCount(), s.skippedCount(),
                s.unrecordedCount(), s.adherenceRate(), s.recordingRate());
    }

    @Transactional
    public ReportDetailResponse createReport(UUID userId, CreateReportRequest request) {
        validatePeriod(request.periodStart(), request.periodEnd());

        AnalysisRawData rawData = analysisEngine.loadRawData(
                userId, request.periodStart(), request.periodEnd(), request.includeMemoExcerpts());

        // 데이터 품질 검사
        int recordedDays = analysisEngine.countRecordedDays(rawData);
        if (recordedDays < MIN_RECORDED_DAYS) {
            throw new BadRequestException("일지 기록일이 부족하여 리포트를 생성할 수 없습니다. (" + recordedDays + "/" + MIN_RECORDED_DAYS + "일)");
        }

        // 해시 계산 → 기존 리포트 재사용 여부 판단
        String hash = snapshotSerializer.computeHash(rawData);
        String countHash = snapshotSerializer.computeCountHash(rawData);
        Optional<MedicationAnalysisReport> existing = reportRepository
                .findByUser_IdAndPeriodStartAndPeriodEnd(
                        userId, request.periodStart(), request.periodEnd());
        if (existing.isPresent()
                && existing.get().getStatus() == ReportStatus.COMPLETED
                && hash.equals(existing.get().getSourceDataHash())) {
            return ReportDetailResponse.from(existing.get(), false);
        }

        // 스냅샷 생성
        AnalysisSnapshot snapshot = analysisEngine.build(rawData, request.includeMemoExcerpts());
        String snapshotJson = snapshotSerializer.toJson(snapshot);

        // Gemini 호출 (동의한 경우)
        String aiResultJson = null;
        String modelName = null;
        String promptVersion = null;

        if (termService.isAiAnalysisConsented(userId)) {
            GeminiReportClient.GeminiReportResult geminiResult = geminiReportClient.generate(snapshotJson, snapshot);
            if (geminiResult.success()) {
                aiResultJson = geminiResult.aiResultJson();
                modelName = geminiResult.modelName();
                promptVersion = geminiResult.promptVersion();
            }
        }

        // 저장: 기존 엔티티가 있으면 in-place 업데이트(unique constraint 방지), 없으면 신규 생성
        MedicationAnalysisReport saved;
        LocalDateTime now = LocalDateTime.now(clock);
        if (existing.isPresent()) {
            MedicationAnalysisReport report = existing.get();
            report.updateSnapshot(snapshotJson, hash, countHash, now);
            if (aiResultJson != null) {
                report.completeWithAiResult(aiResultJson, modelName, promptVersion);
            } else {
                report.failAiResult();
            }
            saved = reportRepository.save(report);
        } else {
            User userRef = userRepository.getReferenceById(userId);
            saved = reportRepository.save(MedicationAnalysisReport.builder()
                    .user(userRef)
                    .periodStart(request.periodStart())
                    .periodEnd(request.periodEnd())
                    .status(ReportStatus.COMPLETED)
                    .sourceDataHash(hash)
                    .rowCountHash(countHash)
                    .snapshotJson(snapshotJson)
                    .aiResultJson(aiResultJson)
                    .modelName(modelName)
                    .promptVersion(promptVersion)
                    .generatedAt(now)
                    .build());
        }

        return ReportDetailResponse.from(saved, false);
    }

    @Transactional(readOnly = true)
    public ReportDetailResponse getReport(UUID userId, Long reportId) {
        MedicationAnalysisReport report = reportRepository.findByIdAndUser_Id(reportId, userId)
                .orElseThrow(() -> new NotFoundException("리포트를 찾을 수 없습니다."));

        boolean outdated = isOutdated(userId, report);
        return ReportDetailResponse.from(report, outdated);
    }

    @Transactional(readOnly = true)
    public List<ReportListItemResponse> listReports(UUID userId) {
        return reportRepository.findByUser_IdOrderByGeneratedAtDesc(userId).stream()
                .map(report -> ReportListItemResponse.from(report, false))
                .toList();
    }

    // -------------------------------------------------------------------------

    private void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (endDate.isAfter(LocalDate.now(clock))) {
            throw new BadRequestException("미래 날짜는 선택할 수 없습니다.");
        }
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (days < MIN_PERIOD_DAYS) {
            throw new BadRequestException("분석 기간은 최소 " + MIN_PERIOD_DAYS + "일이어야 합니다.");
        }
        if (days > MAX_PERIOD_DAYS) {
            throw new BadRequestException("분석 기간은 최대 " + MAX_PERIOD_DAYS + "일까지 선택할 수 있습니다.");
        }
    }

    private boolean isOutdated(UUID userId, MedicationAnalysisReport report) {
        if (report.getRowCountHash() == null) return false;
        try {
            String currentCountHash = snapshotSerializer.computeCountHash(
                    userId, report.getPeriodStart(), report.getPeriodEnd());
            return !currentCountHash.equals(report.getRowCountHash());
        } catch (Exception e) {
            return false;
        }
    }
}
