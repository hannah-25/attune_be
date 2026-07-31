package attune.medication.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserMedicationLogTest {

    @Test
    void deactivateMarksLogInactive() {
        UserMedicationLog log = UserMedicationLog.builder()
                .status(UserMedicationLogStatus.TAKEN)
                .build();

        log.deactivate();

        assertThat(log.isActive()).isFalse();
    }
}
