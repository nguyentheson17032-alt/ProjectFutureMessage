package com.futuremessage.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnlockSchedulerPropertiesTest {

    @Test
    void acceptsPositiveIntervalAndBatchSize() {
        UnlockSchedulerProperties properties = new UnlockSchedulerProperties(true, Duration.ofSeconds(30), 50);

        assertThat(properties.enabled()).isTrue();
        assertThat(properties.interval()).isEqualTo(Duration.ofSeconds(30));
        assertThat(properties.batchSize()).isEqualTo(50);
    }

    @Test
    void rejectsNonPositiveInterval() {
        assertThatThrownBy(() -> new UnlockSchedulerProperties(true, Duration.ZERO, 50))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("interval");
    }

    @Test
    void rejectsBatchSizeOutOfRange() {
        assertThatThrownBy(() -> new UnlockSchedulerProperties(true, Duration.ofSeconds(30), 0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("batch-size");
        assertThatThrownBy(() -> new UnlockSchedulerProperties(true, Duration.ofSeconds(30), 501))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("batch-size");
    }
}
