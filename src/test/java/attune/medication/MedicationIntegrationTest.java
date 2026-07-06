package attune.medication;

import attune.medication.domain.model.Medication;
import attune.medication.domain.model.MedicationDosage;
import attune.support.IntegrationTest;
import attune.user.domain.model.User;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * medication HTTP->DB 전 구간 통합 테스트.
 */
class MedicationIntegrationTest extends IntegrationTest {

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
