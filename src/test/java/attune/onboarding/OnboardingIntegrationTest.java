package attune.onboarding;

import attune.support.IntegrationTest;
import attune.user.domain.model.User;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * onboarding HTTP->DB 전 구간 통합 테스트.
 */
class OnboardingIntegrationTest extends IntegrationTest {

    @Test
    void fullOnboardingFlowCompletesAndProvidesHistory() throws Exception {
        User user = testUsers.activeUser("onboarding-flow@test.com");
        referenceData.systemJournalTags();
        when(aiTextGenerator.generateJson(anyString())).thenReturn("""
                {
                  "visibleTags": ["깜빡함", "미룸"],
                  "visibleConditionTags": ["평온"],
                  "treatmentGoals": [
                    {"goal": "아침 할 일 적기", "functionalArea": "업무/학업"},
                    {"goal": "알람 바로 시작하기", "functionalArea": "시간관리"},
                    {"goal": "물건 문 앞에 두기", "functionalArea": "생활관리"},
                    {"goal": "대화 전 숨 고르기", "functionalArea": "정서/관계"}
                  ]
                }
                """);

        mockMvc.perform(get("/v1/onboarding/status")
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onboarded").value(false))
                .andExpect(jsonPath("$.skipped").value(false))
                .andExpect(jsonPath("$.resumeStep").value(2));

        mockMvc.perform(post("/v1/onboarding/symptoms")
                        .header("Authorization", testUsers.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "description", "I forget tasks and struggle to start work.",
                                "emotionalEvent", "I felt tense after missing a deadline.",
                                "isQuickOnboarding", false))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.symptomId").isNumber());

        MvcResult asrs = mockMvc.perform(post("/v1/onboarding/asrs")
                        .header("Authorization", testUsers.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("answers", asrsAnswers()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assessmentId").isNumber())
                .andExpect(jsonPath("$.partAScore").value(12))
                .andExpect(jsonPath("$.totalScore").value(36))
                .andReturn();
        String assessmentId = String.valueOf(((Number) JsonPath.read(
                asrs.getResponse().getContentAsString(), "$.assessmentId")).longValue());

        MvcResult recommendations = mockMvc.perform(post("/v1/onboarding/ai-recommendations")
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags[1].trouble").value("깜빡함"))
                .andExpect(jsonPath("$.tags[1].recommended").value(true))
                .andExpect(jsonPath("$.conditionTags[0].name").value("평온"))
                .andExpect(jsonPath("$.conditionTags[0].recommended").value(true))
                .andExpect(jsonPath("$.goals[0].goal").value("아침 할 일 적기"))
                .andReturn();

        Long troubleTagId = firstRecommendedId(
                recommendations.getResponse().getContentAsString(), "$.tags[1].tagId");
        Long conditionTagId = firstRecommendedId(
                recommendations.getResponse().getContentAsString(), "$.conditionTags[0].tagId");

        mockMvc.perform(post("/v1/onboarding/goals")
                        .header("Authorization", testUsers.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "goals", List.of(
                                        Map.of("title", "아침 할 일 적기", "type", "WORK_STUDY"),
                                        Map.of("title", "알람 바로 시작하기", "type", "TIME_MANAGEMENT"),
                                        Map.of("title", "물건 문 앞에 두기", "type", "LIFE_MANAGEMENT"),
                                        Map.of("title", "대화 전 숨 고르기", "type", "EMOTIONAL_SOCIAL")
                                ),
                                "visibleTagIds", List.of(troubleTagId),
                                "visibleConditionTagIds", List.of(conditionTagId)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.goals").isArray())
                .andExpect(jsonPath("$.goals[0].title").value("아침 할 일 적기"))
                .andExpect(jsonPath("$.goals[0].isActive").value(true));

        mockMvc.perform(post("/v1/onboarding/complete")
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isOnboarded").value(true));

        mockMvc.perform(get("/v1/onboarding/status")
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onboarded").value(true))
                .andExpect(jsonPath("$.resumeStep").doesNotExist());

        mockMvc.perform(get("/v1/onboarding/history")
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].id").value(assessmentId))
                .andExpect(jsonPath("$.records[0].inattentionScore").value(18))
                .andExpect(jsonPath("$.records[0].hyperactivityScore").value(18))
                .andExpect(jsonPath("$.records[0].goalCount").value(4));

        mockMvc.perform(get("/v1/onboarding/history/{id}", assessmentId)
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(assessmentId))
                .andExpect(jsonPath("$.symptom.description").value("I forget tasks and struggle to start work."))
                .andExpect(jsonPath("$.goals[0].title").value("아침 할 일 적기"));
    }

    @Test
    void quickOnboardingCompletionAppearsInHistory() throws Exception {
        User user = testUsers.activeUser("quick-onboarding-history@test.com");

        mockMvc.perform(post("/v1/onboarding/symptoms")
                        .header("Authorization", testUsers.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "selectedSymptomTypes", List.of("INATTENTION"),
                                "selectedFunctionalAreas", List.of("TIME_MANAGEMENT"),
                                "isQuickOnboarding", true))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/onboarding/goals")
                        .header("Authorization", testUsers.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "goals", List.of(Map.of("title", "Start the day with a plan", "type", "TIME_MANAGEMENT"))))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/onboarding/complete")
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isOk());

        MvcResult history = mockMvc.perform(get("/v1/onboarding/history")
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].isQuickOnboarding").value(true))
                .andReturn();
        String historyId = JsonPath.read(history.getResponse().getContentAsString(), "$.records[0].id");

        mockMvc.perform(get("/v1/onboarding/history/{id}", historyId)
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(historyId))
                .andExpect(jsonPath("$.doneAt").exists())
                .andExpect(jsonPath("$.symptom.isQuickOnboarding").value(true))
                .andExpect(jsonPath("$.symptom.selectedSymptomTypes[0]").value("INATTENTION"))
                .andExpect(jsonPath("$.symptom.selectedFunctionalAreas[0]").value("TIME_MANAGEMENT"))
                .andExpect(jsonPath("$.goals[0].title").value("Start the day with a plan"));
    }

    @Test
    void skipOnboardingSetsSkippedStatus() throws Exception {
        User user = testUsers.activeUser("onboarding-skip@test.com");

        mockMvc.perform(post("/v1/onboarding/skip")
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/onboarding/status")
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onboarded").value(false))
                .andExpect(jsonPath("$.skipped").value(true))
                .andExpect(jsonPath("$.resumeStep").doesNotExist());
    }

    @Test
    void completeWithoutRequiredStepsReturns400() throws Exception {
        User user = testUsers.activeUser("onboarding-incomplete@test.com");

        mockMvc.perform(post("/v1/onboarding/complete")
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    private List<Map<String, Integer>> asrsAnswers() {
        return java.util.stream.IntStream.rangeClosed(1, 18)
                .mapToObj(questionId -> Map.of("questionId", questionId, "score", 2))
                .toList();
    }

    private Long firstRecommendedId(String body, String path) {
        Number id = JsonPath.read(body, path);
        return id.longValue();
    }
}
