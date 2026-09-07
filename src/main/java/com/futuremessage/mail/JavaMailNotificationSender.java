package com.futuremessage.mail;

import com.futuremessage.config.MailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * Gửi MIME multipart (plain + HTML) qua Spring {@link JavaMailSender}.
 * Local: Mailpit {@code localhost:1025}. Prod: SMTP từ env.
 */
@Component
public class JavaMailNotificationSender implements MailNotificationSender {

    private static final String FROM_PERSONAL = "Future Message";

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    public JavaMailNotificationSender(JavaMailSender mailSender, MailProperties mailProperties) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
    }

    @Override
    public void send(String to, UnlockMailContent content) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            helper.setFrom(new InternetAddress(mailProperties.from(), FROM_PERSONAL, StandardCharsets.UTF_8.name()));
            helper.setTo(to);
            helper.setSubject(content.subject());
            helper.setText(content.textBody(), content.htmlBody());
            mailSender.send(mimeMessage);
        } catch (MessagingException | UnsupportedEncodingException | MailException ex) {
            throw new IllegalStateException("Failed to send unlock notification email to " + to, ex);
        }
    }
}
