package attune.communityBoard;

import attune.support.IntegrationTest;
import attune.user.domain.model.User;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * community/consultation HTTP->DB 전 구간 통합 테스트.
 */
class CommunityConsultationIntegrationTest extends IntegrationTest {

    @Test
    void postAndCommentCrudHonorsOwnership() throws Exception {
        User author = testUsers.activeUser("community-author@test.com");
        User commenter = testUsers.activeUser("community-commenter@test.com");

        Long postId = createPost(author, "Original title", "Original content", "DAILY_LIFE", false);

        mockMvc.perform(get("/v1/community/posts")
                        .header("Authorization", testUsers.bearer(author))
                        .param("q", "Original")
                        .param("category", "DAILY_LIFE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].postId").value(postId))
                .andExpect(jsonPath("$.content[0].isOwner").value(true));

        mockMvc.perform(get("/v1/community/posts/{postId}", postId)
                        .header("Authorization", testUsers.bearer(commenter)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(postId))
                .andExpect(jsonPath("$.isOwner").value(false));

        mockMvc.perform(put("/v1/community/posts/{postId}", postId)
                        .header("Authorization", testUsers.bearer(commenter))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "postCategory", "MEDICATION",
                                "title", "Other update",
                                "content", "not allowed"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/v1/community/posts/{postId}", postId)
                        .header("Authorization", testUsers.bearer(author))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "postCategory", "MEDICATION",
                                "title", "Updated title",
                                "content", "Updated content"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.postCategory").value("MEDICATION"));

        MvcResult commentResult = mockMvc.perform(post("/v1/community/posts/{postId}/comments", postId)
                        .header("Authorization", testUsers.bearer(commenter))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", "first reply",
                                "isAnonymous", true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.anonNickname").value("익명"))
                .andExpect(jsonPath("$.isPostAuthor").value(false))
                .andReturn();
        Number commentId = JsonPath.read(commentResult.getResponse().getContentAsString(), "$.commentId");

        mockMvc.perform(get("/v1/community/posts/{postId}/comments", postId)
                        .header("Authorization", testUsers.bearer(author)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].commentId").value(commentId.longValue()))
                .andExpect(jsonPath("$[0].content").value("first reply"))
                .andExpect(jsonPath("$[0].isOwner").value(false));

        mockMvc.perform(patch("/v1/community/comments/{commentId}", commentId.longValue())
                        .header("Authorization", testUsers.bearer(commenter))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", "edited reply",
                                "isAnonymous", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentId").value(commentId.longValue()));

        mockMvc.perform(delete("/v1/community/comments/{commentId}", commentId.longValue())
                        .header("Authorization", testUsers.bearer(author)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/v1/community/comments/{commentId}", commentId.longValue())
                        .header("Authorization", testUsers.bearer(commenter)))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/v1/community/posts/{postId}", postId)
                        .header("Authorization", testUsers.bearer(author)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/community/posts/{postId}", postId)
                        .header("Authorization", testUsers.bearer(author)))
                .andExpect(status().isNotFound());
    }

    @Test
    void consultationScheduleQuestionsAndResultFlow() throws Exception {
        User user = testUsers.activeUser("consultation-flow@test.com");
        LocalDateTime consultationDate = LocalDate.now().plusDays(3).atTime(14, 0);
        Long consultationId = createConsultation(user, consultationDate);

        MvcResult questionResult = mockMvc.perform(post("/v1/consultations/{consultationId}/questions", consultationId)
                        .header("Authorization", testUsers.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("text", "What should I track?"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("What should I track?"))
                .andReturn();
        Number questionId = JsonPath.read(questionResult.getResponse().getContentAsString(), "$.questionId");

        mockMvc.perform(get("/v1/consultations/{consultationId}/questions", consultationId)
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].questionId").value(questionId.longValue()));

        mockMvc.perform(patch("/v1/consultations/{consultationId}/result", consultationId)
                        .header("Authorization", testUsers.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "doctorAdvice", "Keep a sleep log",
                                "prescriptionNote", "Maintain dose",
                                "nextTreatmentGoal", "Review focus at work"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consultationId").value(consultationId));

        mockMvc.perform(get("/v1/consultations/{consultationId}", consultationId)
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.place").value("Seoul clinic"))
                .andExpect(jsonPath("$.doctorAdvice").value("Keep a sleep log"))
                .andExpect(jsonPath("$.prescriptionNote").value("Maintain dose"));

        mockMvc.perform(get("/v1/consultations")
                        .header("Authorization", testUsers.bearer(user))
                        .param("startDate", consultationDate.toLocalDate().toString())
                        .param("endDate", consultationDate.toLocalDate().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consultations[0].consultationId").value(consultationId))
                .andExpect(jsonPath("$.consultations[0].prescriptionNote").value("Maintain dose"));

        mockMvc.perform(patch("/v1/consultations/{consultationId}", consultationId)
                        .header("Authorization", testUsers.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "consultationDate", consultationDate.plusDays(1).toString(),
                                "place", "Busan clinic",
                                "alarmSettings", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consultationId").value(consultationId))
                .andExpect(jsonPath("$.place").value("Busan clinic"));

        mockMvc.perform(delete("/v1/consultations/{consultationId}/questions/{questionId}", consultationId, questionId.longValue())
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/v1/consultations/{consultationId}/result", consultationId)
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/consultations/{consultationId}", consultationId)
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.doctorAdvice").value(nullValue()))
                .andExpect(jsonPath("$.prescriptionNote").value(nullValue()));
    }

    @Test
    void consultationRejectsOtherUserAndInvalidDateRange() throws Exception {
        User owner = testUsers.activeUser("consultation-owner@test.com");
        User other = testUsers.activeUser("consultation-other@test.com");
        LocalDateTime consultationDate = LocalDate.now().plusDays(2).atTime(10, 0);
        Long consultationId = createConsultation(owner, consultationDate);

        mockMvc.perform(get("/v1/consultations/{consultationId}", consultationId)
                        .header("Authorization", testUsers.bearer(other)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/v1/consultations")
                        .header("Authorization", testUsers.bearer(owner))
                        .param("startDate", consultationDate.toLocalDate().plusDays(1).toString())
                        .param("endDate", consultationDate.toLocalDate().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    private Long createPost(User user, String title, String content, String category, boolean anonymous) throws Exception {
        MvcResult result = mockMvc.perform(post("/v1/community/posts")
                        .header("Authorization", testUsers.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "postCategory", category,
                                "title", title,
                                "content", content,
                                "isAnonymous", anonymous))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.postId").isNumber())
                .andReturn();
        Number postId = JsonPath.read(result.getResponse().getContentAsString(), "$.postId");
        return postId.longValue();
    }

    private Long createConsultation(User user, LocalDateTime consultationDate) throws Exception {
        MvcResult result = mockMvc.perform(post("/v1/consultations")
                        .header("Authorization", testUsers.bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "consultationDate", consultationDate.toString(),
                                "place", "Seoul clinic",
                                "doctorName", "Dr. Kim",
                                "isFirstVisit", true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.consultationId").isNumber())
                .andExpect(jsonPath("$.place").value("Seoul clinic"))
                .andReturn();
        Number consultationId = JsonPath.read(result.getResponse().getContentAsString(), "$.consultationId");
        return consultationId.longValue();
    }
}
