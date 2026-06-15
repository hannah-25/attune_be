package attune.medication.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserMedicationTest {

    @Test
    void updateClearsEndAtWhenEndAtFieldIsProvidedAsNull() {
        UserMedication userMedication = UserMedication.builder()
                .isActive(false)
                .startedAt(LocalDate.of(2026, 1, 1))
                .endAt(LocalDate.of(2026, 2, 1))
                .updatedAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build();

        userMedication.update(null, true, true, null);

        assertNull(userMedication.getEndAt());
        assertEquals(true, userMedication.getIsActive());
    }

    @Test
    void updateKeepsEndAtWhenMedicationIsActivated() {
        LocalDate existingEndAt = LocalDate.of(2026, 2, 1);
        UserMedication userMedication = UserMedication.builder()
                .isActive(false)
                .startedAt(LocalDate.of(2026, 1, 1))
                .endAt(existingEndAt)
                .updatedAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build();

        userMedication.update(null, false, true, null);

        assertEquals(existingEndAt, userMedication.getEndAt());
        assertEquals(true, userMedication.getIsActive());
    }

    @Test
    void updateKeepsEndAtWhenEndAtFieldIsOmitted() {
        LocalDate existingEndAt = LocalDate.of(2026, 2, 1);
        UserMedication userMedication = UserMedication.builder()
                .isActive(false)
                .startedAt(LocalDate.of(2026, 1, 1))
                .endAt(existingEndAt)
                .updatedAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build();

        userMedication.update(null, false, null, null);

        assertEquals(existingEndAt, userMedication.getEndAt());
        assertEquals(false, userMedication.getIsActive());
    }
}
