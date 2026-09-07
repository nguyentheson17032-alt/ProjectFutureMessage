package com.futuremessage.scheduler;

import com.futuremessage.config.UnlockSchedulerProperties;
import com.futuremessage.service.UnlockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

/**
 * Trigger định kỳ gọi {@link UnlockService}. Không chứa nghiệp vụ — chỉ lịch chạy + bắt exception
 * để một lần fail không làm chết thread scheduler.
 * <p>
 * Tắt bằng {@code app.scheduler.unlock.enabled=false} (profile test đã tắt).
 */
@Component
@ConditionalOnProperty(prefix = "app.scheduler.unlock", name = "enabled", havingValue = "true", matchIfMissing = true)
public class UnlockScheduler implements SchedulingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(UnlockScheduler.class);

    private final UnlockService unlockService;
    private final UnlockSchedulerProperties properties;

    public UnlockScheduler(UnlockService unlockService, UnlockSchedulerProperties properties) {
        this.unlockService = unlockService;
        this.properties = properties;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addFixedDelayTask(this::runSafely, properties.interval());
    }

    void runSafely() {
        try {
            int unlocked = unlockService.unlockDueMessages();
            if (unlocked > 0) {
                log.info("Unlock job finished: {} message(s) now AVAILABLE", unlocked);
            } else {
                log.debug("Unlock job finished: no due LOCKED messages");
            }
        } catch (RuntimeException ex) {
            log.error("Unlock job failed; will retry on next interval", ex);
        }
    }
}
