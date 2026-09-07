package com.futuremessage.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Bật Spring Scheduling và cấp {@link TaskScheduler} 1 thread.
 * <p>
 * Pool size = 1 để job unlock và job email không chạy chồng lên nhau trên cùng một instance.
 * Nhiều instance app vẫn an toàn nhờ {@code SELECT ... FOR UPDATE SKIP LOCKED} ở repository.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties({UnlockSchedulerProperties.class, NotificationSchedulerProperties.class})
public class SchedulerConfig {

    private static final Logger log = LoggerFactory.getLogger(SchedulerConfig.class);

    @Bean
    TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("fm-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.setErrorHandler(error -> log.error("Scheduled task failed", error));
        scheduler.initialize();
        return scheduler;
    }
}
