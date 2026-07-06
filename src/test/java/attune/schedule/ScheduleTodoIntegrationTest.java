package attune.schedule;

import attune.support.IntegrationTest;
import attune.user.domain.model.User;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * schedule/todo HTTP->DB 전 구간 통합 테스트.
 */
class ScheduleTodoIntegrationTest extends IntegrationTest {

    @Test
    void categoryAndScheduleCrudWithAlarms() throws Exception {
        User user = testUsers.activeUser("schedule-flow@test.com");
        Long categoryId = createCategory(user, "Work", "#3366ff");
        LocalDateTime start = LocalDate.now().plusDays(1).atTime(10, 0);
        LocalDateTime end = start.plusHours(1);
        LocalDateTime firstAlarm = start.minusMinutes(30);

        mockMvc.perform(get("/v1/schedule-categories")
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[0].categoryId").value(categoryId))
                .andExpect(jsonPath("$.categories[0].categoryName").value("Work"))
                .andExpect(jsonPath("$.categories[0].color").value("#3366ff"));

        mockMvc.perform(patch("/v1/schedule-categories/{categoryId}", categoryId)
                        .header("Authorization", testUsers.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "categoryName", "Deep Work",
                                "color", "#114488"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(categoryId))
                .andExpect(jsonPath("$.categoryName").value("Deep Work"));

        Long scheduleId = createSchedule(user, categoryId, "Focus block", start, end, List.of(firstAlarm));

        mockMvc.perform(get("/v1/schedules")
                        .header("Authorization", testUsers.bearer(user))
                        .param("startDate", start.toLocalDate().toString())
                        .param("endDate", start.toLocalDate().toString())
                        .param("source", "MANUAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schedules[0].scheduleId").value(scheduleId))
                .andExpect(jsonPath("$.schedules[0].title").value("Focus block"))
                .andExpect(jsonPath("$.schedules[0].color").value("#114488"))
                .andExpect(jsonPath("$.schedules[0].source").value("MANUAL"));

        mockMvc.perform(get("/v1/schedules/{scheduleId}", scheduleId)
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Focus block"))
                .andExpect(jsonPath("$.categoryId").value(categoryId))
                .andExpect(jsonPath("$.alarmEnabled").value(true))
                .andExpect(jsonPath("$.alarms[0]").value(jsonDateTime(firstAlarm)));

        LocalDateTime secondAlarm = start.minusMinutes(10);
        mockMvc.perform(put("/v1/schedules/{scheduleId}/alarms", scheduleId)
                        .header("Authorization", testUsers.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "alarmEnabled", true,
                                "alarmedAt", List.of(secondAlarm.toString())))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduleId").value(scheduleId))
                .andExpect(jsonPath("$.alarms[0]").value(jsonDateTime(secondAlarm)));

        mockMvc.perform(delete("/v1/schedules/{scheduleId}", scheduleId)
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/schedules/{scheduleId}", scheduleId)
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isNotFound());
    }

    @Test
    void anotherUsersScheduleCannotBeReadOrUpdated() throws Exception {
        User owner = testUsers.activeUser("schedule-owner@test.com");
        User other = testUsers.activeUser("schedule-other@test.com");
        Long categoryId = createCategory(owner, "Owner", "#111111");
        LocalDateTime start = LocalDate.now().plusDays(1).atTime(9, 0);
        Long scheduleId = createSchedule(owner, categoryId, "Owner schedule", start, start.plusHours(1), List.of());

        mockMvc.perform(get("/v1/schedules/{scheduleId}", scheduleId)
                        .header("Authorization", testUsers.bearer(other)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        mockMvc.perform(put("/v1/schedules/{scheduleId}/alarms", scheduleId)
                        .header("Authorization", testUsers.bearer(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("alarmEnabled", false))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void tooManyScheduleAlarmsReturns400() throws Exception {
        User user = testUsers.activeUser("schedule-alarms@test.com");
        Long categoryId = createCategory(user, "Alarm", "#aa0000");
        LocalDateTime start = LocalDate.now().plusDays(1).atTime(11, 0);

        mockMvc.perform(post("/v1/schedules")
                        .header("Authorization", testUsers.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Too many alarms",
                                "categoryId", categoryId,
                                "isAllDay", false,
                                "startTime", start.toString(),
                                "endTime", start.plusHours(1).toString(),
                                "alarmEnabled", true,
                                "alarmedAt", List.of(
                                        start.minusMinutes(10).toString(),
                                        start.minusMinutes(20).toString(),
                                        start.minusMinutes(30).toString(),
                                        start.minusMinutes(40).toString())))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void todoCreateListDetailAndUpdate() throws Exception {
        User user = testUsers.activeUser("todo-flow@test.com");
        LocalDateTime dueAt = LocalDate.now().plusDays(1).atTime(15, 0);

        mockMvc.perform(post("/v1/todos")
                        .header("Authorization", testUsers.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "text", "write note",
                                "dueAt", dueAt.toString(),
                                "isAllDay", false))))
                .andExpect(status().isCreated());

        MvcResult listResult = mockMvc.perform(get("/v1/todos")
                        .header("Authorization", testUsers.bearer(user))
                        .param("date", dueAt.toLocalDate().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todos[0].text").value("write note"))
                .andExpect(jsonPath("$.todos[0].isCompleted").value(false))
                .andReturn();
        Number todoId = JsonPath.read(listResult.getResponse().getContentAsString(), "$.todos[0].todoId");

        mockMvc.perform(get("/v1/todos/{todoId}", todoId.longValue())
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todoId").value(todoId.longValue()))
                .andExpect(jsonPath("$.text").value("write note"));

        mockMvc.perform(patch("/v1/todos/{todoId}", todoId.longValue())
                        .header("Authorization", testUsers.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "text", "write final note",
                                "isCompleted", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todoId").value(todoId.longValue()))
                .andExpect(jsonPath("$.text").value("write final note"));

        mockMvc.perform(get("/v1/todos/{todoId}", todoId.longValue())
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("write final note"))
                .andExpect(jsonPath("$.isCompleted").value(true));
    }

    private Long createCategory(User user, String name, String color) throws Exception {
        MvcResult result = mockMvc.perform(post("/v1/schedule-categories")
                        .header("Authorization", testUsers.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "categoryName", name,
                                "color", color))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryId").isNumber())
                .andReturn();
        Number categoryId = JsonPath.read(result.getResponse().getContentAsString(), "$.categoryId");
        return categoryId.longValue();
    }

    private Long createSchedule(
            User user,
            Long categoryId,
            String title,
            LocalDateTime start,
            LocalDateTime end,
            List<LocalDateTime> alarms
    ) throws Exception {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("title", title);
        body.put("description", "integration schedule");
        body.put("categoryId", categoryId);
        body.put("place", "desk");
        body.put("isAllDay", false);
        body.put("startTime", start.toString());
        body.put("endTime", end.toString());
        body.put("alarmEnabled", !alarms.isEmpty());
        body.put("alarmedAt", alarms.stream().map(LocalDateTime::toString).toList());

        MvcResult result = mockMvc.perform(post("/v1/schedules")
                        .header("Authorization", testUsers.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scheduleId").isNumber())
                .andExpect(jsonPath("$.title").value(title))
                .andReturn();
        Number scheduleId = JsonPath.read(result.getResponse().getContentAsString(), "$.scheduleId");
        return scheduleId.longValue();
    }

    private String jsonDateTime(LocalDateTime value) {
        return value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
    }
}
