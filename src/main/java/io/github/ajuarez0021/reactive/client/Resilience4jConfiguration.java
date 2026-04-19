package io.github.ajuarez0021.reactive.client;



import io.github.ajuarez0021.reactive.client.autoconfigure.RestHttpClient;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;

import java.time.Duration;


/**
 * The Class Resilience4jConfiguration.
 */
public class Resilience4jConfiguration {
    
    /** The circuit breaker registry. */
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    
    /** The retry registry. */
    private final RetryRegistry retryRegistry;
    
    /** The rate limiter registry. */
    private final RateLimiterRegistry rateLimiterRegistry;
    
    /** The time limiter registry. */
    private final TimeLimiterRegistry timeLimiterRegistry;

    /**
     * Instantiates a new resilience4j configuration.
     */
    public Resilience4jConfiguration() {
        this.circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        this.retryRegistry = RetryRegistry.ofDefaults();
        this.rateLimiterRegistry = RateLimiterRegistry.ofDefaults();
        this.timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
    }

    /**
     * Creates the circuit breaker.
     *
     * @param name the name
     * @param config the config
     * @param errorHandler the error handler
     * @return the circuit breaker
     */
    public CircuitBreaker createCircuitBreaker(String name, RestHttpClient config, ErrorHandler errorHandler) {
        io.github.ajuarez0021.reactive.client.autoconfigure.CircuitBreakerConfig cbConfig = config.circuitBreaker();

        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(cbConfig.failureRateThreshold())
                .slidingWindowSize(cbConfig.slidingWindowSize())
                .minimumNumberOfCalls(cbConfig.minimumNumberOfCalls())
                .waitDurationInOpenState(Duration.ofMillis(cbConfig.waitDurationInOpenState()))
                .permittedNumberOfCallsInHalfOpenState(cbConfig.permittedNumberOfCallsInHalfOpenState())
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordException(throwable -> {
                    // WebClientResponseException: delegate directly to the error handler
                    if (throwable instanceof org.springframework.web.reactive.function.client.WebClientResponseException webEx) {
                        return errorHandler.shouldRecordAsFailure(
                                org.springframework.http.HttpStatusCode.valueOf(webEx.getStatusCode().value()));
                    }
                    // ClientException wrapping an HttpStatusCodeException (e.g. 4xx converted by handleError):
                    // delegate to the same predicate so 4xx errors never trip the circuit breaker.
                    if (throwable instanceof ClientException ce
                            && ce.getCause() instanceof org.springframework.web.client.HttpStatusCodeException hsce) {
                        return errorHandler.shouldRecordAsFailure(hsce.getStatusCode());
                    }
                    // Network errors and unexpected exceptions are always recorded as failures.
                    return true;
                })
                .build();

        return circuitBreakerRegistry.circuitBreaker(name, circuitBreakerConfig);
    }

    /**
     * Creates the retry.
     *
     * @param name the name
     * @param config the config
     * @param errorHandler the error handler
     * @return the retry
     */
    public Retry createRetry(String name, RestHttpClient config, ErrorHandler errorHandler) {
        io.github.ajuarez0021.reactive.client.autoconfigure.RetryConfig retryConf = config.retry();

        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(retryConf.maxRetries() + 1)
                .intervalFunction(io.github.resilience4j.core.IntervalFunction.ofExponentialBackoff(
                        retryConf.retryDelay(),
                        retryConf.exponentialBackoffMultiplier()
                ))
                .retryOnException(throwable -> {
                    if (throwable instanceof org.springframework.web.reactive.function.client.WebClientResponseException webEx) {
                        return errorHandler.shouldRetry(
                                org.springframework.http.HttpStatusCode.valueOf(webEx.getStatusCode().value()));
                    }
                    if (throwable instanceof org.springframework.web.client.HttpStatusCodeException httpEx) {
                        return errorHandler.shouldRetry(httpEx.getStatusCode());
                    }
                    if (throwable instanceof ClientException) {
                        return false;
                    }
                    return true;
                })
                .build();

        return retryRegistry.retry(name, retryConfig);
    }

    /**
     * Creates the rate limiter.
     *
     * @param name the name
     * @param config the config
     * @return the rate limiter
     */
    public RateLimiter createRateLimiter(String name, RestHttpClient config) {
        io.github.ajuarez0021.reactive.client.autoconfigure.RateLimiterConfig rlConfig = config.rateLimiter();

        RateLimiterConfig rateLimiterConfig = RateLimiterConfig.custom()
                .limitForPeriod(rlConfig.limitForPeriod())
                .limitRefreshPeriod(Duration.ofMillis(rlConfig.limitRefreshPeriod()))
                .timeoutDuration(Duration.ofMillis(rlConfig.timeoutDuration()))
                .build();

        return rateLimiterRegistry.rateLimiter(name, rateLimiterConfig);
    }

    /**
     * Creates the time limiter.
     *
     * @param name the name
     * @param config the config
     * @return the time limiter
     */
    public TimeLimiter createTimeLimiter(String name, RestHttpClient config) {
        io.github.ajuarez0021.reactive.client.autoconfigure.TimeLimiterConfig tlConfig = config.timeLimiter();

        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(tlConfig.timeout()))
                .cancelRunningFuture(true)
                .build();

        return timeLimiterRegistry.timeLimiter(name, timeLimiterConfig);
    }
}
