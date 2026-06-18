package attune.medicationAnalysis.domain.repository;

import attune.medicationAnalysis.domain.model.MedicationAnalysisReport;
import attune.medicationAnalysis.domain.model.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MedicationAnalysisReportRepository extends JpaRepository<MedicationAnalysisReport, Long> {

    Optional<MedicationAnalysisReport> findByUser_IdAndPeriodStartAndPeriodEnd(
            UUID userId, LocalDate periodStart, LocalDate periodEnd);

    List<MedicationAnalysisReport> findByUser_IdOrderByGeneratedAtDesc(UUID userId);

    Optional<MedicationAnalysisReport> findByIdAndUser_Id(Long id, UUID userId);

    List<MedicationAnalysisReport> findByUser_IdAndStatus(UUID userId, ReportStatus status);
}
