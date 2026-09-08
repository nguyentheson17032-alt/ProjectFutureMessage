package com.futuremessage.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CorsPropertiesTest {

    @Test
    void splitsCommaSeparatedOrigins() {
        CorsProperties properties = new CorsProperties(
                List.of("http://localhost:3000,http://localhost:5173")
        );

        assertThat(properties.allowedOrigins())
                .containsExactly("http://localhost:3000", "http://localhost:5173");
    }

    @Test
    void trimsAndDropsBlanks() {
        CorsProperties properties = new CorsProperties(List.of(" http://localhost:5173 ", " ", "http://127.0.0.1:5173"));

        assertThat(properties.allowedOrigins())
                .containsExactly("http://localhost:5173", "http://127.0.0.1:5173");
    }
}
