package attune.medication;

import attune.medication.domain.model.Medication;
import attune.medication.domain.model.MedicationDosage;
import attune.support.IntegrationTest;
import attune.user.domain.model.User;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * medication HTTP->DB 전 구간 통합 테스트.
 */
class MedicationIntegrationTest extends IntegrationTest {

    private static final ZoneId SERVER_ZONE = ZoneId.of("Asia/Seoul");
    /** 해외 출장 시나리오용 체류지. KST와 날짜가 갈릴 만큼 시차가 크다. */
    private static final ZoneId TRAVEL_ZONE = ZoneId.of("America/New_York");

    @Test
    void searchesAndReadsStandardMedicationDetail() throws Exception {
        User user = testUsers.activeUser("med-standard@test.com");
        Medication medication = referenceData.standardMedication("Concerta");
        MedicationDosage dosage = referenceData.dosage(medication, "18.00");

        mockMvc.perform(get("/v1/medications")
                        .header("Authorization", testUsers.bearer(user))
                        .param("q", "concer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].medicationId").value(medication.getId()))
                .andExpect(jsonPath("$[0].name").value("Concerta"))
                .andExpect(jsonPath("$[0].ingredient").value("Concerta-generic"))
                .andExpect(jsonPath("$[0].dosageOptions[0].dosageId").value(dosage.getId()))
                .andExpect(jsonPath("$[0].dosageOptions[0].amount").value(18.00));

        mockMvc.perform(get("/v1/medications/standards/{medicationId}", medication.getId())
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Concerta"))
                .andExpect(jsonPath("$.ingredient").value("Concerta-generic"))
                .andExpect(jsonPath("$.indications").value("test-effect"))
                .andExpect(jsonPath("$.dosageOptions[0].dosageId").value(dosage.getId()));
    }

    @Test
    void createsUserMedicationQuickLogsAndReadsLogs() throws Exception {
        User user = testUsers.activeUser("med-flow@test.com");
        Medication medication = referenceData.standardMedication("MedFlow");
        MedicationDosage dosage = referenceData.dosage(medication, "10.00");
        LocalDate today = LocalDate.now();

        Long userMedicationId = createUserMedication(user, dosage.getId(), today, "09:00:00", "morning");
        Long scheduleId = firstScheduleId(user);

        mockMvc.perform(post("/v1/user-medications/{userMedicationId}/log/quick", userMedicationId)
                        .header("Authorization", testUsers.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "action", "TAKEN",
                                "scheduleId", scheduleId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.logId").isNumber())
                .andExpect(jsonPath("$.action").value("TAKEN"));

        mockMvc.perform(get("/v1/user-medications/{userMedicationId}/logs", userMedicationId)
                        .header("Authorization", testUsers.bearer(user))
                        .param("startDate", today.toString())
                        .param("endDate", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userMedicationId").value(userMedicationId))
                .andExpect(jsonPath("$.logs[0].status").value("TAKEN"))
                .andExpect(jsonPath("$.logs[0].scheduleId").value(scheduleId));

        mockMvc.perform(get("/v1/user-medications/logs")
                        .header("Authorization", testUsers.bearer(user))
                        .param("startDate", today.toString())
                        .param("endDate", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logs[0].userMedicationId").value(userMedicationId))
                .andExpect(jsonPath("$.logs[0].scheduleId").value(scheduleId))
                .andExpect(jsonPath("$.logs[0].name").value("MedFlow"))
                .andExpect(jsonPath("$.logs[0].taken").value(true));
    }

    /**
     * 오프라인 큐는 at-least-once로 재전송한다. 서버가 쓰기를 적용한 뒤 응답이 유실되면
     * 동일한 TAKEN 요청이 한 번 더 도착한다. 이때 기록이 뒤집히지 않아야 한다.
     */
    @Test
    void replayedTakenKeepsLogTaken() throws Exception {
        User user = testUsers.activeUser("med-replay-taken@test.com");
        Medication medication = referenceData.standardMedication("ReplayTaken");
        MedicationDosage dosage = referenceData.dosage(medication, "10.00");
        LocalDate today = LocalDate.now();

        Long userMedicationId = createUserMedication(user, dosage.getId(), today, "09:00:00", "morning");
        Long scheduleId = firstScheduleId(user);

        quickLog(user, userMedicationId, "TAKEN", scheduleId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.action").value("TAKEN"));

        quickLog(user, userMedicationId, "TAKEN", scheduleId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.logId").isNumber())
                .andExpect(jsonPath("$.action").value("TAKEN"));

        assertSingleActiveLog(user, userMedicationId, today, "TAKEN");
    }

    @Test
    void replayedSkippedKeepsLogSkipped() throws Exception {
        User user = testUsers.activeUser("med-replay-skipped@test.com");
        Medication medication = referenceData.standardMedication("ReplaySkipped");
        MedicationDosage dosage = referenceData.dosage(medication, "10.00");
        LocalDate today = LocalDate.now();

        Long userMedicationId = createUserMedication(user, dosage.getId(), today, "09:00:00", "morning");
        Long scheduleId = firstScheduleId(user);

        quickLog(user, userMedicationId, "SKIPPED", scheduleId).andExpect(status().isCreated());
        quickLog(user, userMedicationId, "SKIPPED", scheduleId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.action").value("SKIPPED"));

        assertSingleActiveLog(user, userMedicationId, today, "SKIPPED");
    }

    @Test
    void skippedThenTakenUpdatesLogInPlace() throws Exception {
        User user = testUsers.activeUser("med-replay-transition@test.com");
        Medication medication = referenceData.standardMedication("ReplayTransition");
        MedicationDosage dosage = referenceData.dosage(medication, "10.00");
        LocalDate today = LocalDate.now();

        Long userMedicationId = createUserMedication(user, dosage.getId(), today, "09:00:00", "morning");
        Long scheduleId = firstScheduleId(user);

        quickLog(user, userMedicationId, "SKIPPED", scheduleId).andExpect(status().isCreated());
        quickLog(user, userMedicationId, "TAKEN", scheduleId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.action").value("TAKEN"));

        assertSingleActiveLog(user, userMedicationId, today, "TAKEN");
    }

    @Test
    void explicitCancelDeactivatesLogAndReplayedCancelIsNoOp() throws Exception {
        User user = testUsers.activeUser("med-replay-cancel@test.com");
        Medication medication = referenceData.standardMedication("ReplayCancel");
        MedicationDosage dosage = referenceData.dosage(medication, "10.00");
        LocalDate today = LocalDate.now();

        Long userMedicationId = createUserMedication(user, dosage.getId(), today, "09:00:00", "morning");
        Long scheduleId = firstScheduleId(user);

        quickLog(user, userMedicationId, "TAKEN", scheduleId).andExpect(status().isCreated());

        quickLog(user, userMedicationId, "CANCEL", scheduleId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.action").value("CANCEL"));

        // 재전송된 CANCEL은 이미 비활성인 로그를 다시 취소하려 한다. 404가 아니라 no-op이어야 한다.
        quickLog(user, userMedicationId, "CANCEL", scheduleId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.action").value("CANCEL"));

        mockMvc.perform(get("/v1/user-medications/{userMedicationId}/logs", userMedicationId)
                        .header("Authorization", testUsers.bearer(user))
                        .param("startDate", today.toString())
                        .param("endDate", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logs").isEmpty());
    }

    /**
     * 취소한 뒤 같은 날 다시 복용할 수 있어야 한다.
     * (활성 로그 유니크 제약이 비활성 로그 때문에 막히면 안 된다.)
     */
    @Test
    void canRetakeAfterCancelOnSameDay() throws Exception {
        User user = testUsers.activeUser("med-replay-retake@test.com");
        Medication medication = referenceData.standardMedication("ReplayRetake");
        MedicationDosage dosage = referenceData.dosage(medication, "10.00");
        LocalDate today = LocalDate.now();

        Long userMedicationId = createUserMedication(user, dosage.getId(), today, "09:00:00", "morning");
        Long scheduleId = firstScheduleId(user);

        quickLog(user, userMedicationId, "TAKEN", scheduleId).andExpect(status().isCreated());
        quickLog(user, userMedicationId, "CANCEL", scheduleId).andExpect(status().isCreated());
        quickLog(user, userMedicationId, "TAKEN", scheduleId).andExpect(status().isCreated());

        assertSingleActiveLog(user, userMedicationId, today, "TAKEN");
    }

    /**
     * 자정 직전에 오프라인으로 기록한 복용을 자정 이후에 재전송해도, 복용일은 기록 시각을 따라야 한다.
     * 서버 수신 시각을 쓰면 복용이 다음 날로 넘어가고 재전송이 별도 로그를 하나 더 만든다.
     */
    @Test
    void replayUsesClientTakenAtSoDoseStaysOnItsOwnDay() throws Exception {
        User user = testUsers.activeUser("med-replay-midnight@test.com");
        Medication medication = referenceData.standardMedication("ReplayMidnight");
        MedicationDosage dosage = referenceData.dosage(medication, "10.00");
        LocalDate yesterday = LocalDate.now().minusDays(1);

        Long userMedicationId = createUserMedication(user, dosage.getId(), yesterday, "09:00:00", "morning");
        Long scheduleId = firstScheduleId(user);

        // 어제 23:50에 오프라인으로 기록한 복용이 오늘 재전송된다.
        Instant tappedAt = yesterday.atTime(23, 50).atZone(SERVER_ZONE).toInstant();

        quickLogAt(user, userMedicationId, "TAKEN", scheduleId, tappedAt).andExpect(status().isCreated());
        quickLogAt(user, userMedicationId, "TAKEN", scheduleId, tappedAt).andExpect(status().isCreated());

        // 어제 하루에 활성 로그 1건. 오늘로 넘어가지 않는다.
        assertSingleActiveLog(user, userMedicationId, yesterday, "TAKEN");
        mockMvc.perform(get("/v1/user-medications/{userMedicationId}/logs", userMedicationId)
                        .header("Authorization", testUsers.bearer(user))
                        .param("startDate", LocalDate.now().toString())
                        .param("endDate", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logs").isEmpty());
    }

    @Test
    void quickLogRejectsFutureAndStaleTakenAt() throws Exception {
        User user = testUsers.activeUser("med-replay-bounds@test.com");
        Medication medication = referenceData.standardMedication("ReplayBounds");
        MedicationDosage dosage = referenceData.dosage(medication, "10.00");

        Long userMedicationId = createUserMedication(user, dosage.getId(), LocalDate.now().minusDays(30), "09:00:00", "morning");
        Long scheduleId = firstScheduleId(user);

        // 클라이언트 시계가 크게 앞선 경우
        quickLogAt(user, userMedicationId, "TAKEN", scheduleId, Instant.now().plus(Duration.ofHours(1)))
                .andExpect(status().isBadRequest());

        // 너무 오래 남아 있던 큐 항목
        quickLogAt(user, userMedicationId, "TAKEN", scheduleId, Instant.now().minus(Duration.ofDays(8)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 여행 중 복용일은 체류지 현지 날짜로 귀속돼야 한다.
     *
     * 뉴욕 저녁에 먹은 약은 KST로 환산하면 다음 날이다. 서버 고정 KST로 복용일을 계산하던
     * 이전 구현은 이 복용을 하루 뒤 날짜에 기록했다.
     */
    @Test
    void doseTakenAbroadIsAttributedToLocalDateNotKst() throws Exception {
        User user = testUsers.activeUser("med-travel-ny@test.com");
        Medication medication = referenceData.standardMedication("TravelNY");
        MedicationDosage dosage = referenceData.dosage(medication, "10.00");

        // 출장으로 체류지 timezone을 뉴욕으로 변경한다.
        updateTimezone(user, TRAVEL_ZONE.getId());

        LocalDate nyYesterday = LocalDate.now(TRAVEL_ZONE).minusDays(1);
        Long userMedicationId = createUserMedication(
                user, dosage.getId(), nyYesterday.minusDays(1), "09:00:00", "morning");
        Long scheduleId = firstScheduleId(user);

        // 뉴욕 어제 20:00. KST로 환산하면 하루 뒤가 된다.
        Instant tappedAt = nyYesterday.atTime(20, 0).atZone(TRAVEL_ZONE).toInstant();
        LocalDate kstDate = tappedAt.atZone(SERVER_ZONE).toLocalDate();
        // 전제: 두 날짜가 실제로 갈린다. 갈리지 않으면 이 테스트는 아무것도 증명하지 못한다.
        assertThat(kstDate).isEqualTo(nyYesterday.plusDays(1));

        quickLogAt(user, userMedicationId, "TAKEN", scheduleId, tappedAt).andExpect(status().isCreated());

        // 뉴욕 현지 날짜에 귀속된다.
        assertSingleActiveLog(user, userMedicationId, nyYesterday, "TAKEN");

        // KST 날짜에는 남지 않는다. 이전 구현은 여기에 기록했다.
        mockMvc.perform(get("/v1/user-medications/{userMedicationId}/logs", userMedicationId)
                        .header("Authorization", testUsers.bearer(user))
                        .param("startDate", kstDate.toString())
                        .param("endDate", kstDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logs").isEmpty());
    }

    @Test
    void anotherUsersMedicationCannotBeLoggedOrRead() throws Exception {
        User owner = testUsers.activeUser("med-owner@test.com");
        User other = testUsers.activeUser("med-other@test.com");
        Medication medication = referenceData.standardMedication("OwnerMedication");
        MedicationDosage dosage = referenceData.dosage(medication, "5.00");
        Long userMedicationId = createUserMedication(owner, dosage.getId(), LocalDate.now(), "08:30:00", "owner");

        mockMvc.perform(get("/v1/user-medications/{userMedicationId}/logs", userMedicationId)
                        .header("Authorization", testUsers.bearer(other)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        mockMvc.perform(post("/v1/user-medications/{userMedicationId}/log/quick", userMedicationId)
                        .header("Authorization", testUsers.bearer(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "action", "TAKEN",
                                "scheduleId", 999L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void periodLogsRejectInvalidDateRange() throws Exception {
        User user = testUsers.activeUser("med-range@test.com");

        mockMvc.perform(get("/v1/user-medications/logs")
                        .header("Authorization", testUsers.bearer(user))
                        .param("startDate", LocalDate.now().toString())
                        .param("endDate", LocalDate.now().minusDays(1).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    private void updateTimezone(User user, String timezone) throws Exception {
        mockMvc.perform(patch("/v1/users/settings")
                        .header("Authorization", testUsers.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("timezone", timezone))))
                .andExpect(status().isOk());
    }

    private ResultActions quickLog(User user, Long userMedicationId, String action, Long scheduleId) throws Exception {
        return mockMvc.perform(post("/v1/user-medications/{userMedicationId}/log/quick", userMedicationId)
                .header("Authorization", testUsers.bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "action", action,
                        "scheduleId", scheduleId))));
    }

    /** 오프라인 큐가 재전송하듯 클라이언트 기록 시각(takenAt)을 함께 보낸다. */
    private ResultActions quickLogAt(
            User user, Long userMedicationId, String action, Long scheduleId, Instant takenAt
    ) throws Exception {
        return mockMvc.perform(post("/v1/user-medications/{userMedicationId}/log/quick", userMedicationId)
                .header("Authorization", testUsers.bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "action", action,
                        "scheduleId", scheduleId,
                        "takenAt", takenAt.toString()))));
    }

    private void assertSingleActiveLog(User user, Long userMedicationId, LocalDate date, String status) throws Exception {
        mockMvc.perform(get("/v1/user-medications/{userMedicationId}/logs", userMedicationId)
                        .header("Authorization", testUsers.bearer(user))
                        .param("startDate", date.toString())
                        .param("endDate", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logs.length()").value(1))
                .andExpect(jsonPath("$.logs[0].status").value(status));
    }

    private Long createUserMedication(
            User user,
            Long dosageId,
            LocalDate startedAt,
            String doseTime,
            String label
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/v1/user-medications")
                        .header("Authorization", testUsers.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "medicationDosageId", dosageId,
                                "startedAt", startedAt.toString(),
                                "schedules", java.util.List.of(Map.of(
                                        "doseTime", doseTime,
                                        "label", label))))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userMedicationId").isNumber())
                .andExpect(jsonPath("$.isActive").value(true))
                .andReturn();
        Number userMedicationId = JsonPath.read(result.getResponse().getContentAsString(), "$.userMedicationId");
        return userMedicationId.longValue();
    }

    private Long firstScheduleId(User user) throws Exception {
        MvcResult result = mockMvc.perform(get("/v1/user-medications")
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].schedules[0].scheduleId").isNumber())
                .andReturn();
        Number scheduleId = JsonPath.read(result.getResponse().getContentAsString(), "$[0].schedules[0].scheduleId");
        return scheduleId.longValue();
    }
}
