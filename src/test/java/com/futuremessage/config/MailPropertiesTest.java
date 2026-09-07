package com.futuremessage.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MailPropertiesTest {

    @Test
    void trimsFromAndInboxUrl() {
        MailProperties properties = new MailProperties("  noreply@futuremessage.local  ", "  http://localhost:3000/inbox  ");

        assertThat(properties.from()).isEqualTo("noreply@futuremessage.local");
        assertThat(properties.inboxUrl()).isEqualTo("http://localhost:3000/inbox");
    }

    @Test
    void rejectsBlankFrom() {
        assertThatThrownBy(() -> new MailProperties("  ", "http://localhost:3000/inbox"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("from");
    }

    @Test
    void rejectsBlankInboxUrl() {
        assertThatThrownBy(() -> new MailProperties("noreply@futuremessage.local", ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("inbox-url");
    }
}
