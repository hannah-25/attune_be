package attune.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class ManagementServerProfileConfigTest {

    @Test
    void devProfileBindsManagementServerToLoopbackPort() {
        Properties properties = load("application-dev.yml");

        assertThat(properties.getProperty("management.server.port"))
                .isEqualTo("${MANAGEMENT_SERVER_PORT:8081}");
        assertThat(properties.getProperty("management.server.address"))
                .isEqualTo("${MANAGEMENT_SERVER_ADDRESS:127.0.0.1}");
        assertThat(properties.getProperty("management.metrics.tags.environment"))
                .isEqualTo("dev");
    }

    @Test
    void prodProfileBindsManagementServerToLoopbackPort() {
        Properties properties = load("application-prod.yml");

        assertThat(properties.getProperty("management.server.port"))
                .isEqualTo("${MANAGEMENT_SERVER_PORT:8081}");
        assertThat(properties.getProperty("management.server.address"))
                .isEqualTo("${MANAGEMENT_SERVER_ADDRESS:127.0.0.1}");
        assertThat(properties.getProperty("management.metrics.tags.environment"))
                .isEqualTo("prod");
    }

    private Properties load(String path) {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource(path));
        Properties properties = factory.getObject();
        return properties == null ? new Properties() : properties;
    }
}
