package attune.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Actuator 접근 제어 검증.
 * - health/liveness/readiness probe는 공개(배포 게이트·컨테이너 폴링용).
 * - 배포 게이트는 readiness(= readinessState + db, Redis 비의존)를 폴링한다.
 * - metrics 등 나머지 actuator는 외부 노출 차단(401).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ActuatorSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void aggregateHealthIsPubliclyReachable() throws Exception {
        // 인증 없이 접근 가능해야 한다. Redis 등 일부 의존성이 없는 환경에선 503일 수 있으므로
        // 보안 차단(401/403)이 아니라는 점만 보장한다. 배포 게이트는 readiness probe를 사용한다.
        int httpStatus = mockMvc.perform(get("/actuator/health"))
                .andReturn().getResponse().getStatus();
        assertThat(httpStatus)
                .isIn(HttpStatus.OK.value(), HttpStatus.SERVICE_UNAVAILABLE.value());
    }

    @Test
    void livenessProbeIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk());
    }

    @Test
    void readinessProbeIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk());
    }

    @Test
    void metricsIsNotPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
    }
}
