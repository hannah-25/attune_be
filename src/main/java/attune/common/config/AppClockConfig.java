package attune.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class AppClockConfig {

    @Bean
    @Primary
    Clock appClock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}
