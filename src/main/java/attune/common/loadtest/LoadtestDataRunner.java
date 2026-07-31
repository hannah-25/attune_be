package attune.common.loadtest;

import attune.medication.domain.model.Medication;
import attune.medication.domain.model.MedicationDosage;
import attune.medication.domain.model.UserMedication;
import attune.medication.domain.model.UserMedicationSchedule;
import attune.medication.domain.repository.MedicationDosageRepository;
import attune.medication.domain.repository.MedicationRepository;
import attune.medication.domain.repository.UserMedicationRepository;
import attune.medication.domain.repository.UserMedicationScheduleRepository;
import attune.user.application.UserDataDeletionExecutor;
import attune.user.domain.model.User;
import attune.user.domain.model.UserSetting;
import attune.user.domain.model.UserStatus;
import attune.user.domain.model.UserType;
import attune.user.domain.repository.UserRepository;
import attune.user.domain.repository.UserSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** Creates disposable, active accounts only when loadtest.data.action is explicitly set. */
@Component
@Profile("loadtest")
@RequiredArgsConstructor
class LoadtestDataRunner implements CommandLineRunner {
    static final String EMAIL_PREFIX = "loadtest-";
    static final String PASSWORD = "AttuneLoadtest!1";
    private static final int USER_COUNT = 100;
    private static final String MEDICATION_NAME = "__loadtest_medication__";

    private final Environment environment;
    private final UserRepository userRepository;
    private final UserSettingRepository userSettingRepository;
    private final MedicationRepository medicationRepository;
    private final MedicationDosageRepository medicationDosageRepository;
    private final UserMedicationRepository userMedicationRepository;
    private final UserMedicationScheduleRepository scheduleRepository;
    private final UserDataDeletionExecutor deletionExecutor;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        String action = environment.getProperty("loadtest.data.action", "none");
        if ("seed".equals(action)) seed();
        if ("cleanup".equals(action)) cleanup();
    }

    @Transactional
    void seed() {
        cleanup();
        Medication medication = medicationRepository.save(Medication.builder().name(MEDICATION_NAME).build());
        MedicationDosage dosage = medicationDosageRepository.save(MedicationDosage.builder()
                .medication(medication).amount(BigDecimal.ONE).isActive(true).build());
        LocalDateTime now = LocalDateTime.now();
        for (int index = 1; index <= USER_COUNT; index++) {
            User user = userRepository.save(User.builder()
                    .email(EMAIL_PREFIX + String.format("%03d", index) + "@attune.invalid")
                    .password(passwordEncoder.encode(PASSWORD)).nickname("loadtest" + index)
                    .userType(UserType.USER).userStatus(UserStatus.ACTIVE).createdAt(now).build());
            userSettingRepository.save(UserSetting.createDefault(user));
            UserMedication userMedication = userMedicationRepository.save(UserMedication.builder()
                    .user(user).medicationDosage(dosage).isActive(true).alarmActive(false)
                    .startedAt(LocalDate.now()).createdAt(now).updatedAt(now).build());
            scheduleRepository.save(UserMedicationSchedule.builder().userMedication(userMedication)
                    .doseTime(LocalTime.NOON).label("loadtest").isActive(true).build());
        }
    }

    @Transactional
    void cleanup() {
        List<User> users = userRepository.findByEmailStartingWith(EMAIL_PREFIX);
        users.forEach(user -> deletionExecutor.deleteAllUserData(user.getId()));
        medicationRepository.findByNameContainingIgnoreCaseOrGenericNameContainingIgnoreCaseOrderByIdAsc(MEDICATION_NAME, MEDICATION_NAME)
                .stream().filter(medication -> MEDICATION_NAME.equals(medication.getName())).forEach(medication -> {
                    medicationDosageRepository.findByMedicationIdAndIsActiveTrueOrderByAmountAscIdAsc(medication.getId())
                            .forEach(medicationDosageRepository::delete);
                    medicationRepository.delete(medication);
                });
    }
}
