package com.futuremessage.web;

import com.futuremessage.domain.Message;
import com.futuremessage.domain.MessageStatus;
import com.futuremessage.domain.RecipientType;
import com.futuremessage.domain.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MessageMapperTest {

    private final User sender = user("11111111-1111-1111-1111-111111111111", "ada@example.com", "Ada");
    private final User recipient = user("22222222-2222-2222-2222-222222222222", "bob@example.com", "Bob");

    @Test
    void senderSeesLockedContent() {
        Message message = lockedMessageToOther();

        assertThat(MessageMapper.visibleContent(message, sender)).isEqualTo("secret");
        assertThat(MessageMapper.toResponse(message, sender).content()).isEqualTo("secret");
    }

    @Test
    void recipientDoesNotSeeLockedContent() {
        Message message = lockedMessageToOther();

        assertThat(MessageMapper.visibleContent(message, recipient)).isNull();
        assertThat(MessageMapper.toSummary(message, recipient).content()).isNull();
    }

    @Test
    void recipientSeesContentWhenAvailable() {
        Message message = lockedMessageToOther();
        message.setStatus(MessageStatus.AVAILABLE);

        assertThat(MessageMapper.visibleContent(message, recipient)).isEqualTo("secret");
    }

    @Test
    void strangerDoesNotSeeContent() {
        Message message = lockedMessageToOther();
        User stranger = user("33333333-3333-3333-3333-333333333333", "eve@example.com", "Eve");

        assertThat(MessageMapper.visibleContent(message, stranger)).isNull();
    }

    private Message lockedMessageToOther() {
        return Message.builder()
                .id(UUID.randomUUID())
                .sender(sender)
                .recipientEmail(recipient.getEmail())
                .recipientUser(recipient)
                .recipientType(RecipientType.OTHER)
                .title("Title")
                .content("secret")
                .unlockAt(Instant.parse("2030-01-01T00:00:00Z"))
                .status(MessageStatus.LOCKED)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }

    private static User user(String id, String email, String name) {
        return User.builder()
                .id(UUID.fromString(id))
                .email(email)
                .passwordHash("hash")
                .displayName(name)
                .build();
    }
}
