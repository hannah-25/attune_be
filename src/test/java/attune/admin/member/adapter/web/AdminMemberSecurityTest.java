package attune.admin.member.adapter.web;

import attune.admin.member.application.AdminMemberService;
import attune.common.config.SecurityConfig;
import attune.common.filter.JwtAuthenticationFilter;
import attune.common.security.SecurityErrorResponseWriter;
import attune.common.util.JwtProvider;
import attune.user.application.UserPermanentDeletionService;
import attune.user.application.UserSoftDeletionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminMemberController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, SecurityErrorResponseWriter.class})
class AdminMemberSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminMemberService adminMemberService;
    @MockitoBean
    private UserPermanentDeletionService userPermanentDeletionService;
    @MockitoBean
    private UserSoftDeletionService userSoftDeletionService;
    @MockitoBean
    private JwtProvider jwtProvider;

    @Test
    void unauthenticatedRequestReturnsCommon401Response() throws Exception {
        mockMvc.perform(get("/v1/admin/members"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
    }

    @Test
    void normalUserReturnsCommon403Response() throws Exception {
        mockMvc.perform(get("/v1/admin/members").with(user("user").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("관리자 권한이 필요합니다."));
    }

    @Test
    void invalidPageRangeReturns400() throws Exception {
        mockMvc.perform(get("/v1/admin/members")
                        .param("page", "-1")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void invalidStatusReturns400() throws Exception {
        mockMvc.perform(get("/v1/admin/members")
                        .param("status", "withdraw")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
