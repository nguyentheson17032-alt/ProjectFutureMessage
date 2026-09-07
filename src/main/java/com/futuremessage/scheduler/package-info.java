/**
 * Scheduled jobs.
 * {@link com.futuremessage.scheduler.UnlockScheduler} → {@link com.futuremessage.service.UnlockService}
 * (LOCKED → AVAILABLE).
 * {@link com.futuremessage.scheduler.NotificationScheduler} → {@link com.futuremessage.service.NotificationService}
 * (gửi email, retry FAILED, bỏ qua SENT).
 */
package com.futuremessage.scheduler;
