package com.futuremessage.mail;

import com.futuremessage.config.MailProperties;
import com.futuremessage.domain.Message;
import com.futuremessage.domain.MessageStatus;
import com.futuremessage.domain.NotificationStatus;
import com.futuremessage.domain.RecipientType;
import com.futuremessage.domain.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UnlockMailComposerTest {

    private static final Instant UNLOCK_AT = Instant.parse("2026-09-07T04:00:00Z");

    private final UnlockMailComposer composer = new UnlockMailComposer(
            new MailProperties("noreply@futuremessage.local", "http://localhost:3000/inbox")
    );

    @Test
    void composeIncludesSenderTitleUnlockTimeAndInboxLinkButNotContent() {
        Message message = availableMessage();

        UnlockMailContent content = composer.compose(message);

        assertThat(content.subject()).contains("To future you");
        assertThat(content.textBody()).contains("Ada");
        assertThat(content.textBody()).contains("To future you");
        assertThat(content.textBody()).contains("Asia/Ho_Chi_Minh");
        assertThat(content.textBody()).contains("http://localhost:3000/inbox");
        assertThat(content.textBody()).doesNotContain("secret body must stay private");
        assertThat(content.htmlBody()).contains("<strong>Ada</strong>");
        assertThat(content.htmlBody()).contains("http://localhost:3000/inbox");
        assertThat(content.htmlBody()).doesNotContain("secret body must stay private");
    }

    @Test
    void htmlEscapesTitleToPreventInjection() {
        Message message = availableMessage();
        message.setTitle("<script>alert(1)</script>");

        UnlockMailContent content = composer.compose(message);

        assertThat(content.htmlBody()).contains("&lt;script&gt;alert(1)&lt;/script&gt;");
        assertThat(content.htmlBody()).doesNotContain("<script>alert(1)</script>");
        assertThat(content.subject()).contains("<script>alert(1)</script>");
    }

    private Message availableMessage() {
        User sender = User.builder()
                .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .email("ada@example.com")
                .passwordHash("hash")
                .displayName("Ada")
                .build();
        return Message.builder()
                .id(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
                .sender(sender)
                .recipientEmail("bob@example.com")
                .recipientType(RecipientType.OTHER)
                .title("To future you")
                .content("secret body must stay private")
                .unlockAt(UNLOCK_AT)
                .status(MessageStatus.AVAILABLE)
                .notificationStatus(NotificationStatus.PENDING)
                .build();
    }
}
