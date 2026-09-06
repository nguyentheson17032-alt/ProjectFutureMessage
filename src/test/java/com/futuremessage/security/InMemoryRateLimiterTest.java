package com.futuremessage.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRateLimiterTest {

    @Test
    void allowsUpToMaxRequestsThenRejectsUntilWindowResets() {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(2, Duration.ofMinutes(1));
        Instant now = Instant.parse("2026-09-06T01:00:00Z");

        assertThat(limiter.tryAcquire("127.0.0.1", now)).isTrue();
        assertThat(limiter.tryAcquire("127.0.0.1", now.plusSeconds(1))).isTrue();
        assertThat(limiter.tryAcquire("127.0.0.1", now.plusSeconds(2))).isFalse();
        assertThat(limiter.tryAcquire("10.0.0.2", now)).isTrue();
        assertThat(limiter.tryAcquire("127.0.0.1", now.plus(Duration.ofMinutes(1)))).isTrue();
    }
}
