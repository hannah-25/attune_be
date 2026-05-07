package attune.medication.domain.application;

import attune.common.error.notfound.DrugNotFoundException;
import attune.medication.domain.application.dto.response.DrugDetailResponse;
import attune.medication.domain.repository.MedicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class DrugService {

    private final MedicationRepository medicationRepository;

    @Transactional(readOnly = true)
    public DrugDetailResponse getDrug(Long standardDrugId) {
        return medicationRepository.findById(standardDrugId)
                .map(DrugDetailResponse::from)
                .orElseThrow(DrugNotFoundException::new);
    }
}
