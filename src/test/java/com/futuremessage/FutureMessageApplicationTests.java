package com.futuremessage;

import com.futuremessage.config.TimezoneConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Clock;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FutureMessageApplicationTests {

    @Autowired
    private Clock clock;

    @Test
    void contextLoads() {
    }

    @Test
    void clockUsesVietnamTimezone() {
        assertThat(clock.getZone()).isEqualTo(TimezoneConfig.APP_ZONE);
        assertThat(ZoneId.systemDefault()).isEqualTo(TimezoneConfig.APP_ZONE);
    }
}
