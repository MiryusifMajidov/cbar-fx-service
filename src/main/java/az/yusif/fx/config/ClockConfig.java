package az.yusif.fx.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {

    @Bean
    Clock clock(FxProperties properties) {
        return Clock.system(ZoneId.of(properties.sync().zone()));
    }
}
