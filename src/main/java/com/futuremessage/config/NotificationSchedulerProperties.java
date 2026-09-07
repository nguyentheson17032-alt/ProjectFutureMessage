package com.futuremessage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Cấu hình job gửi email thông báo message đã mở khóa.
 * <p>
 * Map từ {@code app.scheduler.notification}:
 * <ul>
 *   <li>{@code enabled} — tắt job khi test / bảo trì</li>
 *   <li>{@code interval} — khoảng nghỉ sau khi job vừa chạy xong (fixed delay)</li>
 *   <li>{@code batchSize} — số email tối đa mỗi lần chạy</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "app.scheduler.notification")
public record NotificationSchedulerProperties(
        boolean enabled,
        Duration interval,
        int batchSize
) {

    public NotificationSchedulerProperties {
        if (interval == null || interval.isZero() || interval.isNegative()) {
            throw new IllegalStateException("app.scheduler.notification.interval must be positive");
        }
        if (batchSize < 1 || batchSize > 500) {
            throw new IllegalStateException("app.scheduler.notification.batch-size must be between 1 and 500");
        }
    }
}
