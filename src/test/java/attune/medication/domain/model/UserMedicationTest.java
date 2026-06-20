package attune.medication.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserMedicationTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 21, 0, 0);

    @Test
    void updateClearsEndAtWhenEndAtFieldIsProvidedAsNull() {
        UserMedication userMedication = UserMedication.builder()
                .isActive(false)
                .startedAt(LocalDate.of(2026, 1, 1))
                .endAt(LocalDate.of(2026, 2, 1))
                .updatedAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build();

        userMedication.update(null, true, true, null, NOW);

        assertNull(userMedication.getEndAt());
        assertEquals(true, userMedication.getIsActive());
        assertEquals(NOW, userMedication.getUpdatedAt());
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

        userMedication.update(null, false, true, null, NOW);

        assertEquals(existingEndAt, userMedication.getEndAt());
        assertEquals(true, userMedication.getIsActive());
        assertEquals(NOW, userMedication.getUpdatedAt());
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

        userMedication.update(null, false, null, null, NOW);

        assertEquals(existingEndAt, userMedication.getEndAt());
        assertEquals(false, userMedication.getIsActive());
        assertEquals(NOW, userMedication.getUpdatedAt());
    }
}
