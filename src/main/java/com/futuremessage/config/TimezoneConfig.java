package com.futuremessage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;
import java.util.TimeZone;

@Configuration
public class TimezoneConfig {

    public static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Bean
    Clock clock() {
        TimeZone.setDefault(TimeZone.getTimeZone(APP_ZONE));
        return Clock.system(APP_ZONE);
    }
}
