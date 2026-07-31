package attune.admin;

import attune.support.IntegrationTest;
import attune.user.domain.model.User;
import attune.user.domain.model.UserStatus;
import attune.user.domain.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * admin HTTP->DB 전 구간 통합 테스트.
 */
class AdminIntegrationTest extends IntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void userRoleCannotAccessAdminEndpoints() throws Exception {
        User user = testUsers.activeUser("admin-denied@test.com");

        mockMvc.perform(get("/v1/admin/members")
                        .header("Authorization", testUsers.bearer(user)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void adminCanListMembersChangeStatusAndReadAuditLogs() throws Exception {
        User admin = testUsers.activeAdmin("admin-member@test.com");
        User member = testUsers.activeUser("member-target@test.com");

        mockMvc.perform(get("/v1/admin/members")
                        .header("Authorization", testUsers.bearer(admin))
                        .param("query", "member-target")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members[0].id").value(member.getId().toString()))
                .andExpect(jsonPath("$.members[0].email").value(member.getEmail()))
                .andExpect(jsonPath("$.summary.active").value(2));

        mockMvc.perform(post("/v1/admin/members/{memberId}/status", member.getId())
                        .header("Authorization", testUsers.bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "SUSPENDED",
                                "reason", "policy violation review"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(member.getId().toString()))
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        assertThat(userRepository.findById(member.getId()).orElseThrow().getUserStatus())
                .isEqualTo(UserStatus.SUSPENDED);

        mockMvc.perform(get("/v1/admin/audit-logs")
                        .header("Authorization", testUsers.bearer(admin))
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("STATUS_CHANGED"))
                .andExpect(jsonPath("$[0].administrator").value(admin.getEmail()))
                .andExpect(jsonPath("$[0].reason").value("policy violation review"));
    }

    @Test
    void adminNoticeCrudIsVisibleThroughPublicNoticeApi() throws Exception {
        User admin = testUsers.activeAdmin("admin-notice@test.com");

        MvcResult created = mockMvc.perform(post("/v1/admin/notices")
                        .header("Authorization", testUsers.bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Service notice",
                                "content", "Initial content",
                                "isPinned", true,
                                "sendNotification", false,
                                "sendEmail", false))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.noticeId").isNumber())
                .andExpect(jsonPath("$.title").value("Service notice"))
                .andReturn();
        Number noticeId = JsonPath.read(created.getResponse().getContentAsString(), "$.noticeId");

        mockMvc.perform(get("/v1/notices")
                        .param("q", "Service")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].noticeId").value(noticeId.longValue()))
                .andExpect(jsonPath("$.content[0].isPinned").value(true));

        mockMvc.perform(patch("/v1/admin/notices/{noticeId}", noticeId.longValue())
                        .header("Authorization", testUsers.bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Updated notice",
                                "content", "Updated content",
                                "isPinned", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noticeId").value(noticeId.longValue()))
                .andExpect(jsonPath("$.title").value("Updated notice"));

        mockMvc.perform(get("/v1/notices/{noticeId}", noticeId.longValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated notice"))
                .andExpect(jsonPath("$.content").value("Updated content"))
                .andExpect(jsonPath("$.isPinned").value(false));

        mockMvc.perform(delete("/v1/admin/notices/{noticeId}", noticeId.longValue())
                        .header("Authorization", testUsers.bearer(admin)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/notices/{noticeId}", noticeId.longValue()))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminCanCreateAndListTerms() throws Exception {
        User admin = testUsers.activeAdmin("admin-term@test.com");
        LocalDateTime createdAt = LocalDateTime.now().minusMinutes(1);
        LocalDateTime effectiveAt = LocalDateTime.now().plusDays(1);

        MvcResult created = mockMvc.perform(post("/v1/admin/terms")
                        .header("Authorization", testUsers.bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Privacy update",
                                "version", 3,
                                "content", "updated privacy policy",
                                "type", "PRIVACY_POLICY",
                                "createdAt", createdAt.toString(),
                                "effectiveDate", effectiveAt.toString(),
                                "sendEmail", false))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.version").value(3))
                .andExpect(jsonPath("$.type").value("PRIVACY_POLICY"))
                .andReturn();
        Number termId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/v1/admin/terms")
                        .header("Authorization", testUsers.bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(termId.longValue()))
                .andExpect(jsonPath("$[0].type").value("PRIVACY_POLICY"))
                .andExpect(jsonPath("$[0].version").value(3));
    }
}
