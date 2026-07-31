package attune.support;

import attune.journal.domain.model.JournalTag;
import attune.journal.domain.model.SystemJournalTagDefinitions;
import attune.journal.domain.repository.JournalTagRepository;
import attune.medication.domain.model.Medication;
import attune.medication.domain.model.MedicationDosage;
import attune.medication.domain.repository.MedicationDosageRepository;
import attune.medication.domain.repository.MedicationRepository;
import attune.term.domain.model.Term;
import attune.term.domain.model.TermType;
import attune.term.domain.repository.TermRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 기준(참조) 데이터 픽스처 — ddl-auto: create-drop 환경에는 seed SQL이 없으므로
 * 표준 약(medications)·약관(terms) 같은 기준 데이터는 여기서 넣는다.
 * 도메인 PR마다 임기응변으로 만들지 말 것.
 */
public class ReferenceDataFixture {

    private final MedicationRepository medicationRepository;
    private final MedicationDosageRepository medicationDosageRepository;
    private final JournalTagRepository journalTagRepository;
    private final TermRepository termRepository;

    public ReferenceDataFixture(MedicationRepository medicationRepository,
                                MedicationDosageRepository medicationDosageRepository,
                                JournalTagRepository journalTagRepository,
                                TermRepository termRepository) {
        this.medicationRepository = medicationRepository;
        this.medicationDosageRepository = medicationDosageRepository;
        this.journalTagRepository = journalTagRepository;
        this.termRepository = termRepository;
    }

    public Medication standardMedication(String name) {
        return medicationRepository.save(Medication.builder()
                .name(name)
                .genericName(name + "-generic")
                .effect("test-effect")
                .drugClass("test-class")
                .build());
    }

    public List<JournalTag> systemJournalTags() {
        LocalDateTime now = LocalDateTime.now();
        List<JournalTag> tags = SystemJournalTagDefinitions.all().stream()
                .map(definition -> JournalTag.systemTag(
                        definition.category(),
                        definition.name(),
                        definition.tagType(),
                        definition.defaultVisible(),
                        now))
                .toList();
        return journalTagRepository.saveAll(tags);
    }

    public MedicationDosage dosage(Medication medication, String amount) {
        return medicationDosageRepository.save(MedicationDosage.builder()
                .medication(medication)
                .amount(new BigDecimal(amount))
                .isActive(true)
                .build());
    }

    public Term term(TermType type, int version) {
        return termRepository.save(Term.builder()
                .type(type)
                .version(version)
                .content("test-term-content")
                .effectiveAt(LocalDateTime.now().minusDays(1))
                .createdAt(LocalDateTime.now().minusDays(1))
                .build());
    }
}
