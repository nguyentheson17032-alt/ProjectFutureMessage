package com.futuremessage.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Operational snapshot for the admin dashboard.")
public record AdminStatsResponse(
        long totalUsers,
        MessagesByStatus messagesByStatus,
        NotificationCounts notifications,
        long unlockingWithin24Hours
) {

    public record MessagesByStatus(long locked, long available, long opened, long cancelled) {
    }

    public record NotificationCounts(long pending, long sent, long failed) {
    }
}
