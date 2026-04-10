package com.work.common.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class RestHttpConfigTest {

    @Test
    void resilience4jConfiguration_returnsNonNullBean() {
        RestHttpConfig config = new RestHttpConfig();
        assertThat(config.resilience4jConfiguration()).isNotNull();
    }

    @Test
    void resilience4jConfiguration_returnsNewInstanceEachCall() {
        RestHttpConfig config = new RestHttpConfig();
        assertThat(config.resilience4jConfiguration())
                .isNotSameAs(config.resilience4jConfiguration());
    }
}
