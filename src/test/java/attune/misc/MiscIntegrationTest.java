package attune.misc;

import attune.calendar.application.GoogleCalendarClient.GoogleToken;
import attune.calendar.domain.model.ExternalCalendarEventSnapshot;
import attune.journal.domain.model.DailyStatusLog;
import attune.journal.domain.model.SleepQuality;
import attune.journal.domain.repository.DailyStatusLogRepository;
import attune.medication.domain.model.Medication;
import attune.medication.domain.model.MedicationDosage;
import attune.medication.domain.model.UserMedication;
import attune.medication.domain.model.UserMedicationLog;
import attune.medication.domain.model.UserMedicationLogStatus;
import attune.medication.domain.model.UserMedicationSchedule;
import attune.medication.domain.repository.UserMedicationLogRepository;
import attune.medication.domain.repository.UserMedicationRepository;
import attune.medication.domain.repository.UserMedicationScheduleRepository;
import attune.medicationAnalysis.infrastructure.GeminiReportClient.GeminiReportResult;
import attune.support.IntegrationTest;
import attune.support.domain.model.SupportInquiry;
import attune.support.domain.model.SupportInquiryStatus;
import attune.support.domain.model.SupportInquiryType;
import attune.support.domain.repository.SupportInquiryRepository;
import attune.term.domain.model.TermType;
import attune.user.domain.model.User;
import com.jayway.jsonpath.JsonPath;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 기타 보조 도메인 HTTP->DB 전 구간 통합 테스트.
 */
class MiscIntegrationTest extends IntegrationTest {

    @Autowired
    private SupportInquiryRepository supportInquiryRepository;
    @Autowired
    private UserMedicationRepository userMedicationRepository;
    @Autowired
    private UserMedicationScheduleRepository userMedicationScheduleRepository;
    @Autowired
    private UserMedicationLogRepository userMedicationLogRepository;
    @Autowired
    private DailyStatusLogRepository dailyStatusLogRepository;

    @BeforeEach
    void stubMailSender() {
        when(javaMailSender.createMimeMessage())
                .thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
    }

    @Test
    void supportInquiryStoresRequestAndRejectsInvalidEmail() throws Exception {
        User user = testUsers.activeUser("support-flow@test.com");

        mockMvc.perform(post("/v1/support/inquiries")
                        .header("Authorization", testUsers.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "type", "BUG",
                                "title", "Notification issue",
                                "content", "Push notification did not arrive.",
                                "email", "support-flow@test.com"))))
                .andExpect(status().isCreated());

        SupportInquiry inquiry = supportInquiryRepository.findAll().get(0);
        assertThat(inquiry.getUser().getId()).isEqualTo(user.getId());
        assertThat(inquiry.getType()).isEqualTo(SupportInquiryType.BUG);
        assertThat(inquiry.getStatus()).isEqualTo(SupportInquiryStatus.PENDING);
        assertThat(inquiry.getTitle()).isEqualTo("Notification issue");

        mockMvc.perform(post("/v1/support/inquiries")
                        .header("Authorization", testUsers.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "type", "OTHER",
                                "title", "Bad email",
                                "content", "Invalid email should fail.",
                                "email", "not-an-email"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void aiGenerateUsesMockedGenerator() throws Exception {
        User user = testUsers.activeUser("ai-generate@test.com");
        when(aiTextGenerator.generate("summarize this")).thenReturn("generated summary");

        mockMvc.perform(post("/v1/ai/generate")
                        .header("Authorization", testUsers.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("prompt", "summarize this"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("generated summary"));

        mockMvc.perform(post("/v1/ai/generate")
                        .header("Authorization", testUsers.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("prompt", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void googleCalendarConnectionSyncEventsAndDisconnect() throws Exception {
        User user = testUsers.activeUser("calendar-flow@test.com");
        LocalDateTime eventStart = LocalDate.now().plusDays(5).atTime(9, 0);
        LocalDateTime eventEnd = eventStart.plusHours(1);

        when(googleCalendarClient.exchangeCode("auth-code", "https://app.test/oauth"))
                .thenReturn(new GoogleToken("access-token", "refresh-token", LocalDateTime.now().plusHours(1)));
        when(googleCalendarClient.fetchAccountEmail("access-token"))
                .thenReturn("calendar-user@test.com");
        when(googleCalendarClient.listCalendarIds(any()))
                .thenReturn(List.of("primary"));
        when(googleCalendarClient.listEvents(any(), eq("primary"), any(), any()))
                .thenReturn(List.of(new ExternalCalendarEventSnapshot(
                        "primary",
                        "event-1",
                        "Therapy session",
                        "external calendar event",
                        "Clinic",
                        false,
                        eventStart,
                        eventEnd,
                        LocalDateTime.now(),
                        false,
                        "etag-1"
                )));

        MvcResult connected = mockMvc.perform(post("/v1/calendar-connections/google")
                        .header("Authorization", testUsers.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "authorizationCode", "auth-code",
                                "redirectUri", "https://app.test/oauth"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.connectionId").isNumber())
                .andExpect(jsonPath("$.provider").value("GOOGLE"))
                .andExpect(jsonPath("$.accountEmail").value("calendar-user@test.com"))
                .andReturn();
        Number connectionId = JsonPath.read(connected.getResponse().getContentAsString(), "$.connectionId");

        mockMvc.perform(get("/v1/calendar-connections")
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connections[0].connectionId").value(connectionId.longValue()))
                .andExpect(jsonPath("$.connections[0].active").value(true));

        mockMvc.perform(post("/v1/calendar-connections/{connectionId}/sync", connectionId.longValue())
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connectionId").value(connectionId.longValue()))
                .andExpect(jsonPath("$.syncedCount").value(1));

        mockMvc.perform(get("/v1/calendar/events")
                        .header("Authorization", testUsers.bearer(user))
                        .param("startDate", eventStart.toLocalDate().toString())
                        .param("endDate", eventStart.toLocalDate().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events[0].source").value("EXTERNAL"))
                .andExpect(jsonPath("$.events[0].provider").value("GOOGLE"))
                .andExpect(jsonPath("$.events[0].title").value("Therapy session"))
                .andExpect(jsonPath("$.events[0].editable").value(false));

        mockMvc.perform(delete("/v1/calendar-connections/{connectionId}", connectionId.longValue())
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/calendar-connections")
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connections").isEmpty());
    }

    @Test
    void medicationAnalysisCreatesAiReportAndReusesExistingReport() throws Exception {
        User user = testUsers.activeUser("analysis-flow@test.com");
        LocalDate periodStart = LocalDate.now().minusDays(6);
        LocalDate periodEnd = LocalDate.now();
        seedMedicationAnalysisData(user, periodStart, periodEnd);
        referenceData.term(TermType.AI_ANALYSIS_CONSENT, 1);
        when(geminiReportClient.generate(anyString(), any()))
                .thenReturn(new GeminiReportResult(
                        true,
                        "{\"summary\":\"stable adherence\"}",
                        "gemini-test",
                        "prompt-v1"
                ));

        mockMvc.perform(get("/v1/medication-analysis/availability")
                        .header("Authorization", testUsers.bearer(user))
                        .param("startDate", periodStart.toString())
                        .param("endDate", periodEnd.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.recordedDays").value(7));

        mockMvc.perform(get("/v1/medication-analysis/summary")
                        .header("Authorization", testUsers.bearer(user))
                        .param("startDate", periodStart.toString())
                        .param("endDate", periodEnd.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScheduled").value(7))
                .andExpect(jsonPath("$.takenCount").value(7))
                .andExpect(jsonPath("$.adherenceRate").value(100.0));

        mockMvc.perform(put("/v1/ai-analysis-consent")
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isNoContent());

        MvcResult created = mockMvc.perform(post("/v1/medication-analysis/reports")
                        .header("Authorization", testUsers.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "periodStart", periodStart.toString(),
                                "periodEnd", periodEnd.toString(),
                                "includeMemoExcerpts", false))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportId").isNumber())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.snapshotJson").isNotEmpty())
                .andExpect(jsonPath("$.aiResultJson").value("{\"summary\":\"stable adherence\"}"))
                .andReturn();
        Number reportId = JsonPath.read(created.getResponse().getContentAsString(), "$.reportId");

        mockMvc.perform(get("/v1/medication-analysis/reports/{reportId}", reportId.longValue())
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value(reportId.longValue()))
                .andExpect(jsonPath("$.outdated").value(false));

        mockMvc.perform(get("/v1/medication-analysis/reports")
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reportId").value(reportId.longValue()))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));

        mockMvc.perform(post("/v1/medication-analysis/reports")
                        .header("Authorization", testUsers.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "periodStart", periodStart.toString(),
                                "periodEnd", periodEnd.toString(),
                                "includeMemoExcerpts", false))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportId").value(reportId.longValue()));

        mockMvc.perform(get("/v1/medication-analysis/availability")
                        .header("Authorization", testUsers.bearer(user))
                        .param("startDate", periodStart.toString())
                        .param("endDate", periodStart.plusDays(5).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    private void seedMedicationAnalysisData(User user, LocalDate periodStart, LocalDate periodEnd) {
        Medication medication = referenceData.standardMedication("AnalysisMed");
        MedicationDosage dosage = referenceData.dosage(medication, "18.00");
        LocalDateTime now = LocalDateTime.now();
        UserMedication userMedication = userMedicationRepository.save(UserMedication.builder()
                .user(user)
                .medicationDosage(dosage)
                .isActive(true)
                .alarmActive(true)
                .startedAt(periodStart)
                .createdAt(now)
                .updatedAt(now)
                .build());
        UserMedicationSchedule schedule = userMedicationScheduleRepository.save(UserMedicationSchedule.builder()
                .userMedication(userMedication)
                .doseTime(java.time.LocalTime.of(9, 0))
                .label("morning")
                .isActive(true)
                .build());

        for (LocalDate date = periodStart; !date.isAfter(periodEnd); date = date.plusDays(1)) {
            userMedicationLogRepository.save(UserMedicationLog.builder()
                    .userMedicationSchedule(schedule)
                    .takenAt(date.atTime(9, 5))
                    .status(UserMedicationLogStatus.TAKEN)
                    .isActive(true)
                    .build());
            dailyStatusLogRepository.save(DailyStatusLog.builder()
                    .userId(user.getId())
                    .date(date)
                    .sleepHour(7.0f)
                    .sleepQuality(SleepQuality.GOOD)
                    .ateBreakfast(true)
                    .ateLunch(true)
                    .ateDinner(true)
                    .build());
        }
    }
}
