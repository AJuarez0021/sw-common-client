package io.github.ajuarez0021.reactive.client;

import io.github.ajuarez0021.reactive.client.autoconfigure.CircuitBreakerConfig;
import io.github.ajuarez0021.reactive.client.autoconfigure.RateLimiterConfig;
import io.github.ajuarez0021.reactive.client.autoconfigure.RestHttpClient;
import io.github.ajuarez0021.reactive.client.autoconfigure.TimeLimiterConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.*;

class Resilience4jConfigurationTest {

    @RestHttpClient(
            url = "http://test.example.com",
            name = "resilience-test",
            circuitBreaker = @CircuitBreakerConfig(
                    enabled = true,
                    failureRateThreshold = 60,
                    slidingWindowSize = 10,
                    minimumNumberOfCalls = 5,
                    waitDurationInOpenState = 1000,
                    permittedNumberOfCallsInHalfOpenState = 3
            ),
            rateLimiter = @RateLimiterConfig(
                    enabled = true,
                    limitForPeriod = 50,
                    limitRefreshPeriod = 500,
                    timeoutDuration = 1000
            ),
            timeLimiter = @TimeLimiterConfig(enabled = true, timeout = 5000)
    )
    interface TestClient {}

    private Resilience4jConfiguration configuration;
    private RestHttpClient config;
    private DefaultErrorHandler errorHandler;

    @BeforeEach
    void setUp() {
        configuration = new Resilience4jConfiguration();
        config = TestClient.class.getAnnotation(RestHttpClient.class);
        errorHandler = new DefaultErrorHandler();
    }

    // --- createRetry ---

    @Test
    void createRetry_returnsRetryWithGivenName() {
        Retry retry = configuration.createRetry("my-retry", config, errorHandler);

        assertThat(retry).isNotNull();
        assertThat(retry.getName()).isEqualTo("my-retry");
    }

    @Test
    void createRetry_sameNameReturnsSameInstance() {
        Retry first = configuration.createRetry("shared-retry", config, errorHandler);
        Retry second = configuration.createRetry("shared-retry", config, errorHandler);

        assertThat(first).isSameAs(second);
    }

    @Test
    void createRetry_retryOnException_retryableStatusCodes() {
        Retry retry = configuration.createRetry("test-retry", config, errorHandler);

        // 502 should trigger retry
        boolean shouldRetry502 = retry.getRetryConfig().getExceptionPredicate()
                .test(new org.springframework.web.client.HttpServerErrorException(HttpStatus.BAD_GATEWAY));
        assertThat(shouldRetry502).isTrue();
    }

    @Test
    void createRetry_retryOnException_genericExceptionIsRetried() {
        Retry retry = configuration.createRetry("test-retry-generic", config, errorHandler);

        boolean shouldRetry = retry.getRetryConfig().getExceptionPredicate()
                .test(new RuntimeException("connection reset"));
        assertThat(shouldRetry).isTrue();
    }

    // --- createCircuitBreaker ---

    @Test
    void createCircuitBreaker_returnsCircuitBreakerWithGivenName() {
        CircuitBreaker cb = configuration.createCircuitBreaker("my-cb", config, errorHandler);

        assertThat(cb).isNotNull();
        assertThat(cb.getName()).isEqualTo("my-cb");
    }

    @Test
    void createCircuitBreaker_appliesFailureRateThreshold() {
        CircuitBreaker cb = configuration.createCircuitBreaker("cb-threshold", config, errorHandler);

        assertThat(cb.getCircuitBreakerConfig().getFailureRateThreshold())
                .isEqualTo(60.0f);
    }

    @Test
    void createCircuitBreaker_appliesSlidingWindowSize() {
        CircuitBreaker cb = configuration.createCircuitBreaker("cb-window", config, errorHandler);

        assertThat(cb.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(10);
    }

    @Test
    void createCircuitBreaker_initialStateIsClosed() {
        CircuitBreaker cb = configuration.createCircuitBreaker("cb-state", config, errorHandler);

        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    // --- createRateLimiter ---

    @Test
    void createRateLimiter_returnsRateLimiterWithGivenName() {
        RateLimiter rl = configuration.createRateLimiter("my-rl", config);

        assertThat(rl).isNotNull();
        assertThat(rl.getName()).isEqualTo("my-rl");
    }

    @Test
    void createRateLimiter_appliesLimitForPeriod() {
        RateLimiter rl = configuration.createRateLimiter("rl-limit", config);

        assertThat(rl.getRateLimiterConfig().getLimitForPeriod()).isEqualTo(50);
    }

    // --- createTimeLimiter ---

    @Test
    void createTimeLimiter_returnsTimeLimiterWithGivenName() {
        TimeLimiter tl = configuration.createTimeLimiter("my-tl", config);

        assertThat(tl).isNotNull();
        assertThat(tl.getName()).isEqualTo("my-tl");
    }

    @Test
    void createTimeLimiter_appliesTimeout() {
        TimeLimiter tl = configuration.createTimeLimiter("tl-timeout", config);

        assertThat(tl.getTimeLimiterConfig().getTimeoutDuration().toMillis()).isEqualTo(5000);
    }
}
