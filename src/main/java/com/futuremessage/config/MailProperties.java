package com.futuremessage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình thư gửi đi (không gồm SMTP — SMTP nằm ở {@code spring.mail}).
 * <p>
 * Map từ {@code app.mail}:
 * <ul>
 *   <li>{@code from} — địa chỉ From (local: Mailpit; prod: SMTP env {@code MAIL_FROM})</li>
 *   <li>{@code inboxUrl} — link hướng dẫn người nhận mở hộp thư (frontend)</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(
        String from,
        String inboxUrl
) {

    public MailProperties {
        if (from == null || from.isBlank()) {
            throw new IllegalStateException("app.mail.from must not be blank");
        }
        if (inboxUrl == null || inboxUrl.isBlank()) {
            throw new IllegalStateException("app.mail.inbox-url must not be blank");
        }
        from = from.trim();
        inboxUrl = inboxUrl.trim();
    }
}
