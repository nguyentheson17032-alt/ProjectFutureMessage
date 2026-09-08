package com.futuremessage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.dev-seed")
public record DevSeedProperties(boolean enabled) {
}
