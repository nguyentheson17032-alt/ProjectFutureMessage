/**
 * Application services orchestrate use cases; invariants live on
 * {@link com.futuremessage.domain.Message} and {@link com.futuremessage.domain.MessageRules}.
 * {@link UnlockService} — LOCKED → AVAILABLE khi đến hạn.
 * {@link NotificationService} — gửi email unlock, retry FAILED, không gửi lại SENT.
 * {@link AdminService} — thống kê, user, message vận hành; không mở hộp thư hộ recipient.
 */
package com.futuremessage.service;
