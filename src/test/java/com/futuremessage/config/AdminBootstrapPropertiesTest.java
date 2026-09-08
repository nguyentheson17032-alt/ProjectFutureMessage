package com.futuremessage.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminBootstrapPropertiesTest {

    @Test
    void blankCredentialsAreSkipped() {
        AdminBootstrapProperties properties = new AdminBootstrapProperties("  ", "  ", null);

        assertThat(properties.configured()).isFalse();
        assertThat(properties.displayName()).isEqualTo("Admin");
    }

    @Test
    void normalizesEmailAndDisplayName() {
        AdminBootstrapProperties properties = new AdminBootstrapProperties(
                "  Admin@Example.COM ",
                "adminpass1",
                "  Super Admin  "
        );

        assertThat(properties.configured()).isTrue();
        assertThat(properties.email()).isEqualTo("admin@example.com");
        assertThat(properties.displayName()).isEqualTo("Super Admin");
    }

    @Test
    void rejectsShortPasswordWhenConfigured() {
        assertThatThrownBy(() -> new AdminBootstrapProperties("admin@example.com", "short", "Admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("8");
    }
}
