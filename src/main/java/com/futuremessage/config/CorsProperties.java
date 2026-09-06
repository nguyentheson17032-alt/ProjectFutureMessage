package com.futuremessage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null
                ? List.of()
                : List.copyOf(allowedOrigins.stream().filter(origin -> origin != null && !origin.isBlank()).toList());
    }
}
