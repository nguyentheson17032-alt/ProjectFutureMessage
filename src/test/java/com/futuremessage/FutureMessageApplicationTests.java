package com.futuremessage;

import com.futuremessage.config.TimezoneConfig;
import com.futuremessage.scheduler.NotificationScheduler;
import com.futuremessage.scheduler.UnlockScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.time.Clock;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FutureMessageApplicationTests {

    @Autowired
    private Clock clock;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
    }

    @Test
    void clockUsesVietnamTimezone() {
        assertThat(clock.getZone()).isEqualTo(TimezoneConfig.APP_ZONE);
        assertThat(ZoneId.systemDefault()).isEqualTo(TimezoneConfig.APP_ZONE);
    }

    @Test
    void unlockSchedulerIsDisabledInTests() {
        assertThat(applicationContext.getBeanNamesForType(UnlockScheduler.class)).isEmpty();
    }

    @Test
    void notificationSchedulerIsDisabledInTests() {
        assertThat(applicationContext.getBeanNamesForType(NotificationScheduler.class)).isEmpty();
    }
}
