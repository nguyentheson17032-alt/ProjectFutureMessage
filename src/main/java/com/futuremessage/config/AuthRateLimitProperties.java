package com.futuremessage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.auth")
public record AuthRateLimitProperties(
        int rateLimitRequests,
        Duration rateLimitWindow
) {

    public AuthRateLimitProperties {
        if (rateLimitRequests < 1) {
            throw new IllegalStateException("app.auth.rate-limit-requests must be at least 1");
        }
        if (rateLimitWindow == null || rateLimitWindow.isZero() || rateLimitWindow.isNegative()) {
            throw new IllegalStateException("app.auth.rate-limit-window must be positive");
        }
    }
}
