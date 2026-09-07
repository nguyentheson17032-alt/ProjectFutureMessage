package com.futuremessage.mail;

import com.futuremessage.config.MailProperties;
import com.futuremessage.config.TimezoneConfig;
import com.futuremessage.domain.Message;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * Ghép subject / body (plain + HTML) từ message đã unlock.
 * Không nhúng nội dung tin nhắn vào email.
 */
@Component
public class UnlockMailComposer {

    private static final DateTimeFormatter UNLOCK_AT_FORMAT = DateTimeFormatter
            .ofPattern("HH:mm 'ngày' dd/MM/yyyy")
            .withZone(TimezoneConfig.APP_ZONE);

    private final MailProperties mailProperties;

    public UnlockMailComposer(MailProperties mailProperties) {
        this.mailProperties = mailProperties;
    }

    public UnlockMailContent compose(Message message) {
        String senderName = senderDisplayName(message);
        String title = safe(message.getTitle());
        String unlockAt = message.getUnlockAt() == null ? "" : UNLOCK_AT_FORMAT.format(message.getUnlockAt());
        String inboxUrl = mailProperties.inboxUrl();

        String subject = "Tin nhắn tương lai đã sẵn sàng: " + title;
        String textBody = """
                Xin chào,

                %s đã gửi cho bạn một tin nhắn trên Future Message.

                Tiêu đề: %s
                Thời điểm mở: %s (Asia/Ho_Chi_Minh)

                Tin nhắn đã mở khóa. Đăng nhập và mở hộp thư đến:
                %s

                — Future Message
                """.formatted(senderName, title, unlockAt, inboxUrl);

        String htmlBody = """
                <!DOCTYPE html>
                <html lang="vi">
                <body style="font-family: sans-serif; line-height: 1.5; color: #1a1a1a;">
                  <p>Xin chào,</p>
                  <p><strong>%s</strong> đã gửi cho bạn một tin nhắn trên Future Message.</p>
                  <p>
                    Tiêu đề: <strong>%s</strong><br>
                    Thời điểm mở: %s (Asia/Ho_Chi_Minh)
                  </p>
                  <p>Tin nhắn đã mở khóa. Đăng nhập rồi mở hộp thư đến để đọc.</p>
                  <p><a href="%s">Mở hộp thư đến</a></p>
                  <p style="color:#666;font-size:12px;">Future Message</p>
                </body>
                </html>
                """.formatted(
                escapeHtml(senderName),
                escapeHtml(title),
                escapeHtml(unlockAt),
                escapeHtml(inboxUrl)
        );

        return new UnlockMailContent(subject, textBody, htmlBody);
    }

    private static String senderDisplayName(Message message) {
        if (message.getSender() == null || message.getSender().getDisplayName() == null
                || message.getSender().getDisplayName().isBlank()) {
            return "Ai đó";
        }
        return message.getSender().getDisplayName().trim();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    static String escapeHtml(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
