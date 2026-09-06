package com.futuremessage.config;

import com.futuremessage.security.InMemoryRateLimiter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AuthRateLimitProperties.class)
public class RateLimitConfig {

    @Bean
    InMemoryRateLimiter authRateLimiter(AuthRateLimitProperties properties) {
        return new InMemoryRateLimiter(properties.rateLimitRequests(), properties.rateLimitWindow());
    }
}
