package attune.medicationAnalysis.domain.model;

import attune.user.domain.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "medication_analysis_reports")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MedicationAnalysisReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(nullable = false)
    private LocalDate periodStart;

    @Column(nullable = false)
    private LocalDate periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    @Column(nullable = false)
    private String sourceDataHash;

    private String rowCountHash;

    @Column(columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(columnDefinition = "TEXT")
    private String aiResultJson;

    private String modelName;

    private String promptVersion;

    @Column(nullable = false)
    private LocalDateTime generatedAt;

    public void updateToPending() {
        this.status = ReportStatus.PENDING;
    }

    public void updateSnapshot(String snapshotJson, String sourceDataHash, String rowCountHash, LocalDateTime generatedAt) {
        this.snapshotJson = snapshotJson;
        this.sourceDataHash = sourceDataHash;
        this.rowCountHash = rowCountHash;
        this.generatedAt = generatedAt;
    }

    public void markOutdated() {
        this.status = ReportStatus.OUTDATED;
    }

    public void completeWithAiResult(String aiResultJson, String modelName, String promptVersion) {
        this.aiResultJson = aiResultJson;
        this.modelName = modelName;
        this.promptVersion = promptVersion;
        this.status = ReportStatus.COMPLETED;
    }

    public void failAiResult() {
        this.status = ReportStatus.COMPLETED;
    }
}
