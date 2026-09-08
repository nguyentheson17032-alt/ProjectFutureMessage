package com.futuremessage.web;

import com.futuremessage.domain.Message;
import com.futuremessage.domain.MessageStatus;
import com.futuremessage.domain.NotificationStatus;
import com.futuremessage.domain.RecipientType;
import com.futuremessage.domain.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AdminMapperTest {

    private final User sender = user("11111111-1111-1111-1111-111111111111", "ada@example.com", "Ada");
    private final User recipient = user("22222222-2222-2222-2222-222222222222", "bob@example.com", "Bob");

    @Test
    void summaryNeverIncludesContent() {
        Message message = availableMessage();

        assertThat(AdminMapper.toSummary(message).title()).isEqualTo("Title");
        assertThat(AdminMapper.toSummary(message).senderEmail()).isEqualTo("ada@example.com");
        assertThat(AdminMapper.toSummary(message).notificationStatus()).isEqualTo(NotificationStatus.FAILED);
    }

    @Test
    void detailHidesLockedContentAndShowsAvailableContent() {
        Message locked = lockedMessage();
        assertThat(AdminMapper.toResponse(locked).content()).isNull();

        Message available = availableMessage();
        assertThat(AdminMapper.toResponse(available).content()).isEqualTo("secret");
    }

    private Message lockedMessage() {
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
                .notificationStatus(NotificationStatus.PENDING)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }

    private Message availableMessage() {
        Message message = lockedMessage();
        message.setStatus(MessageStatus.AVAILABLE);
        message.setNotificationStatus(NotificationStatus.FAILED);
        return message;
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
