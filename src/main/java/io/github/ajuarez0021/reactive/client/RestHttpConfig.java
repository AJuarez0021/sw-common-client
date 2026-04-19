package io.github.ajuarez0021.reactive.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The class RestHttpConfig.
 */
@Configuration
public class RestHttpConfig {

    /**
     * The configuration of Resilience4j.
     *
     * @return The object
     */
    @Bean
    public Resilience4jConfiguration resilience4jConfiguration() {
        return new Resilience4jConfiguration();
    }
}
