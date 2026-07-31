package attune.journal;

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
 * journal HTTP->DB 전 구간 통합 테스트.
 */
class JournalIntegrationTest extends IntegrationTest {

    @Test
    void createsTagChecksItAndReadsJournalDetail() throws Exception {
        User user = testUsers.activeUser("journal-flow@test.com");
        LocalDate date = LocalDate.now().minusDays(1);

        Long tagId = createConditionTag(user, "calm-custom");

        mockMvc.perform(post("/v1/journals/tags/{tagId}/checks", tagId)
                        .header("Authorization", testUsers.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("journalDate", date.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tagId").value(tagId))
                .andExpect(jsonPath("$.category").value("CONDITION"))
                .andExpect(jsonPath("$.name").value("calm-custom"))
                .andExpect(jsonPath("$.journalDate").value(date.toString()));

        mockMvc.perform(get("/v1/journals/{date}", date)
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeTags.conditions[0].tagId").value(tagId))
                .andExpect(jsonPath("$.activeTags.conditions[0].condition").value("calm-custom"))
                .andExpect(jsonPath("$.checked.conditions[0].tagId").value(tagId))
                .andExpect(jsonPath("$.checked.conditions[0].condition").value("calm-custom"));
    }

    /**
     * 오프라인 큐가 같은 체크를 재전송해도 행이 하나만 남아야 한다.
     * uk_journal_tag_logs_daily_check를 엔티티에 선언하기 전에는 테스트 DB에 제약이 없어
     * JournalTagLogSaver의 유니크 위반 catch 경로가 실행되지 않았다.
     */
    @Test
    void replayedTagCheckIsIdempotent() throws Exception {
        User user = testUsers.activeUser("journal-replay@test.com");
        LocalDate date = LocalDate.now().minusDays(1);
        Long tagId = createConditionTag(user, "replay-calm");

        for (int attempt = 0; attempt < 2; attempt++) {
            mockMvc.perform(post("/v1/journals/tags/{tagId}/checks", tagId)
                            .header("Authorization", testUsers.bearer(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("journalDate", date.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tagId").value(tagId))
                    .andExpect(jsonPath("$.journalDate").value(date.toString()));
        }

        mockMvc.perform(get("/v1/journals/{date}", date)
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checked.conditions.length()").value(1));
    }

    @Test
    void anotherUsersTagCannotBeChecked() throws Exception {
        User owner = testUsers.activeUser("journal-owner@test.com");
        User other = testUsers.activeUser("journal-other@test.com");
        Long tagId = createConditionTag(owner, "owner-only-calm");

        mockMvc.perform(post("/v1/journals/tags/{tagId}/checks", tagId)
                        .header("Authorization", testUsers.bearer(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "journalDate", LocalDate.now().minusDays(1).toString()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void upsertsMemoAndSleepMealThenReadsJournalDetail() throws Exception {
        User user = testUsers.activeUser("journal-upsert@test.com");
        LocalDate date = LocalDate.now().minusDays(1);

        postMemo(user, date, "first memo");
        postMemo(user, date, "updated memo");
        postSleepMeal(user, date, 7.5F, "GOOD", true, true, false);
        postSleepMeal(user, date, 6.0F, "NORMAL", false, true, true);

        mockMvc.perform(get("/v1/journals/{date}", date)
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checked.memo").value("updated memo"))
                .andExpect(jsonPath("$.checked.sleep.sleepHour").value(6.0))
                .andExpect(jsonPath("$.checked.sleep.sleepQuality").value("NORMAL"))
                .andExpect(jsonPath("$.checked.meal.ateBreakfast").value(false))
                .andExpect(jsonPath("$.checked.meal.ateLunch").value(true))
                .andExpect(jsonPath("$.checked.meal.ateDinner").value(true));
    }

    @Test
    void createsGoalScoresItAndReadsJournalDetail() throws Exception {
        User user = testUsers.activeUser("journal-goal@test.com");
        LocalDate date = LocalDate.now().minusDays(1);

        Long goalId = createGoal(user, date, "review plan");

        mockMvc.perform(post("/v1/journals/{date}/goals", date)
                        .header("Authorization", testUsers.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "goalId", goalId,
                                "score", 8))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goalId").value(goalId))
                .andExpect(jsonPath("$.score").value(8))
                .andExpect(jsonPath("$.journalDate").value(date.toString()));

        mockMvc.perform(get("/v1/journals/{date}", date)
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeTags.goals[0].goalId").value(goalId))
                .andExpect(jsonPath("$.activeTags.goals[0].content").value("review plan"))
                .andExpect(jsonPath("$.checked.goals[0].goalId").value(goalId))
                .andExpect(jsonPath("$.checked.goals[0].score").value(8));
    }

    @Test
    void duplicateUserTagReturns409() throws Exception {
        User user = testUsers.activeUser("journal-duplicate@test.com");
        createConditionTag(user, "duplicate-calm");

        mockMvc.perform(post("/v1/journals/tags")
                        .header("Authorization", testUsers.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "category", "CONDITION",
                                "name", "duplicate-calm",
                                "tagType", "CALM",
                                "visible", true))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    private Long createConditionTag(User user, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/v1/journals/tags")
                        .header("Authorization", testUsers.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "category", "CONDITION",
                                "name", name,
                                "tagType", "CALM",
                                "visible", true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tagId").isNumber())
                .andExpect(jsonPath("$.scope").value("USER"))
                .andReturn();
        Number tagId = JsonPath.read(result.getResponse().getContentAsString(), "$.tagId");
        return tagId.longValue();
    }

    private Long createGoal(User user, LocalDate date, String content) throws Exception {
        MvcResult result = mockMvc.perform(post("/v1/journals/goals")
                        .header("Authorization", testUsers.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", content,
                                "journalDate", date.toString()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.goalId").isNumber())
                .andExpect(jsonPath("$.content").value(content))
                .andReturn();
        Number goalId = JsonPath.read(result.getResponse().getContentAsString(), "$.goalId");
        return goalId.longValue();
    }

    private void postMemo(User user, LocalDate date, String memo) throws Exception {
        mockMvc.perform(post("/v1/journals/{date}/memo", date)
                        .header("Authorization", testUsers.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("memo", memo))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.journalDate").value(date.toString()))
                .andExpect(jsonPath("$.memo").value(memo));
    }

    private void postSleepMeal(
            User user,
            LocalDate date,
            Float sleepHour,
            String sleepQuality,
            boolean ateBreakfast,
            boolean ateLunch,
            boolean ateDinner
    ) throws Exception {
        mockMvc.perform(post("/v1/journals/{date}/sleep-meal", date)
                        .header("Authorization", testUsers.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sleepHour", sleepHour,
                                "sleepQuality", sleepQuality,
                                "ateBreakfast", ateBreakfast,
                                "ateLunch", ateLunch,
                                "ateDinner", ateDinner))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.journalDate").value(date.toString()))
                .andExpect(jsonPath("$.sleepHour").value(sleepHour.doubleValue()))
                .andExpect(jsonPath("$.sleepQuality").value(sleepQuality));
    }
}
