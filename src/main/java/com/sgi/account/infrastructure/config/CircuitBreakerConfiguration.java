package com.sgi.account.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;

/**
 * Configuration class for @Resilience4j components such as Circuit Breaker,
 * Retry, and TimeLimiter for the remittance service.
 */
@Configuration
public class CircuitBreakerConfiguration {

    /**
     * Configures a Circuit Breaker for the remittance service.
     *
     * @param registry the CircuitBreakerRegistry to retrieve the CircuitBreaker from.
     * @return the configured CircuitBreaker instance.
     */
    @Bean
    CircuitBreaker remittanceServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("account-service");
    }

    /**
     * Configures a Retry policy for the remittance service.
     *
     * @param registry the RetryRegistry to retrieve the Retry instance from.
     * @return the configured Retry instance.
     */
    @Bean
    Retry remittanceServiceRetry(RetryRegistry registry) {
        return registry.retry("account-service");
    }

    /**
     * Configures a TimeLimiter for the remittance service.
     *
     * @param registry the TimeLimiterRegistry to retrieve the TimeLimiter from.
     * @return the configured TimeLimiter instance.
     */
    @Bean
    TimeLimiter remittanceServiceTimeLimiter(TimeLimiterRegistry registry) {
        return registry.timeLimiter("account-service");
    }
}
