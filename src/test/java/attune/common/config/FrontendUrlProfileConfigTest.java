package attune.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * app.frontend-url 은 인증 메일·비밀번호 재설정 링크의 base URL 이다(AccountService).
 * 값이 비면 기동은 성공하지만 링크가 상대경로로 발송되므로 조용히 깨진다.
 */
class FrontendUrlProfileConfigTest {

    @Test
    void devProfileDefinesFrontendUrl() {
        assertThat(load("application-dev.yml").getProperty("app.frontend-url"))
                .isEqualTo("https://dev.attune-me.com");
    }

    @Test
    void prodProfileDefinesFrontendUrl() {
        assertThat(load("application-prod.yml").getProperty("app.frontend-url"))
                .isEqualTo("https://attune-me.com");
    }

    private Properties load(String path) {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource(path));
        Properties properties = factory.getObject();
        return properties == null ? new Properties() : properties;
    }
}
