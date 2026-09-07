package com.futuremessage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Cấu hình job unlock message.
 * <p>
 * Map từ {@code app.scheduler.unlock} trong {@code application.yml}:
 * <ul>
 *   <li>{@code enabled} — tắt job khi test / bảo trì, không cần tắt cả {@code @EnableScheduling}</li>
 *   <li>{@code interval} — khoảng nghỉ sau khi job vừa chạy xong (fixed delay, mặc định 30s)</li>
 *   <li>{@code batchSize} — số hàng khóa tối đa mỗi lần chạy, tránh transaction quá lâu</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "app.scheduler.unlock")
public record UnlockSchedulerProperties(
        boolean enabled,
        Duration interval,
        int batchSize
) {

    public UnlockSchedulerProperties {
        if (interval == null || interval.isZero() || interval.isNegative()) {
            throw new IllegalStateException("app.scheduler.unlock.interval must be positive");
        }
        if (batchSize < 1 || batchSize > 500) {
            throw new IllegalStateException("app.scheduler.unlock.batch-size must be between 1 and 500");
        }
    }
}
