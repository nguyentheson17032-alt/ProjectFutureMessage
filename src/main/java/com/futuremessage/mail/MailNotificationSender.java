package com.futuremessage.mail;

/**
 * Gửi email thông báo unlock qua SMTP. Implementation production dùng {@code JavaMailSender}.
 */
public interface MailNotificationSender {

    void send(String to, UnlockMailContent content);
}
