package com.futuremessage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = normalize(allowedOrigins);
    }

    /**
     * YAML {@code a,b} and env {@code CORS_ALLOWED_ORIGINS=a,b} may arrive as one element.
     * Split so each origin is matched independently.
     */
    static List<String> normalize(List<String> allowedOrigins) {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            return List.of();
        }
        return allowedOrigins.stream()
                .filter(origin -> origin != null && !origin.isBlank())
                .flatMap(origin -> Arrays.stream(origin.split(",")))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .distinct()
                .toList();
    }
}
