package attune.consultation.domain.model;

import attune.user.domain.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "consultations")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Consultation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime consultationDate;

    @Column(nullable = false)
    private String place;

    private String doctorName;

    @Column(nullable = false)
    private boolean isFirstVisit;

    @Column(columnDefinition = "TEXT")
    private String summaryReport;  // 확인

    @Column(columnDefinition = "TEXT")
    private String preConsultationNote;

    @Column(columnDefinition = "TEXT")
    private String doctorAdvice;

    @Column(columnDefinition = "TEXT")
    private String prescriptionNote;

    @Column(columnDefinition = "TEXT")
    private String nextTreatmentGoal;

    @Builder.Default
    @Column(nullable = false)
    private boolean alarmSettings = true;

    @Column(nullable = false)
    private boolean isDeleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public void updateSchedule(LocalDateTime consultationDate, String place, Boolean alarmSettings) {
        if (consultationDate != null) this.consultationDate = consultationDate;
        if (place != null) this.place = place;
        if (alarmSettings != null) this.alarmSettings = alarmSettings;
        this.updatedAt = LocalDateTime.now();
    }

    public void updatePreparation(String preConsultationNote) {
        if (preConsultationNote != null) this.preConsultationNote = preConsultationNote;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateResult(String doctorAdvice, String prescriptionNote, String nextTreatmentGoal) {
        if (doctorAdvice != null) this.doctorAdvice = doctorAdvice;
        if (prescriptionNote != null) this.prescriptionNote = prescriptionNote;
        if (nextTreatmentGoal != null) this.nextTreatmentGoal = nextTreatmentGoal;
        this.updatedAt = LocalDateTime.now();
    }

    public void clearResult() {
        this.doctorAdvice = null;
        this.prescriptionNote = null;
        this.nextTreatmentGoal = null;
        this.summaryReport = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void delete() {
        this.isDeleted = true;
        this.updatedAt = LocalDateTime.now();
    }
}