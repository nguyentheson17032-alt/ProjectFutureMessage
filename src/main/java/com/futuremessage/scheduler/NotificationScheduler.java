package com.futuremessage.scheduler;

import com.futuremessage.config.NotificationSchedulerProperties;
import com.futuremessage.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

/**
 * Trigger định kỳ gọi {@link NotificationService}. Không chứa nghiệp vụ — chỉ lịch chạy + bắt exception
 * để một lần fail không làm chết thread scheduler.
 * <p>
 * Tắt bằng {@code app.scheduler.notification.enabled=false} (profile test đã tắt).
 */
@Component
@ConditionalOnProperty(prefix = "app.scheduler.notification", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NotificationScheduler implements SchedulingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);

    private final NotificationService notificationService;
    private final NotificationSchedulerProperties properties;

    public NotificationScheduler(
            NotificationService notificationService,
            NotificationSchedulerProperties properties
    ) {
        this.notificationService = notificationService;
        this.properties = properties;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addFixedDelayTask(this::runSafely, properties.interval());
    }

    void runSafely() {
        try {
            int sent = notificationService.sendPendingNotifications();
            if (sent > 0) {
                log.info("Notification job finished: {} email(s) sent", sent);
            } else {
                log.debug("Notification job finished: no pending notifications");
            }
        } catch (RuntimeException ex) {
            log.error("Notification job failed; will retry on next interval", ex);
        }
    }
}
