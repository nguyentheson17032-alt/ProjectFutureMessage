package com.futuremessage.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MessageTest {

    @Test
    void lockedMessageCanBeEditedAndCancelledButNotOpened() {
        Message message = messageWithStatus(MessageStatus.LOCKED);

        assertThat(message.canEdit()).isTrue();
        assertThat(message.canCancel()).isTrue();
        assertThat(message.canOpen()).isFalse();
        assertThat(message.isContentVisibleToRecipient()).isFalse();
    }

    @Test
    void availableMessageCanBeOpenedAndShowsContentToRecipient() {
        Message message = messageWithStatus(MessageStatus.AVAILABLE);

        assertThat(message.canEdit()).isFalse();
        assertThat(message.canCancel()).isFalse();
        assertThat(message.canOpen()).isTrue();
        assertThat(message.isContentVisibleToRecipient()).isTrue();
    }

    @Test
    void openedMessageIsReadOnly() {
        Message message = messageWithStatus(MessageStatus.OPENED);

        assertThat(message.canEdit()).isFalse();
        assertThat(message.canCancel()).isFalse();
        assertThat(message.canOpen()).isFalse();
        assertThat(message.isContentVisibleToRecipient()).isTrue();
    }

    @Test
    void isAddressedToMatchesRecipientUserOrEmail() {
        User recipient = User.builder()
                .id(UUID.randomUUID())
                .email("recipient@example.com")
                .passwordHash("hash")
                .displayName("Recipient")
                .build();
        Message message = Message.builder()
                .status(MessageStatus.LOCKED)
                .recipientEmail("recipient@example.com")
                .recipientUser(recipient)
                .build();

        assertThat(message.isAddressedTo(recipient)).isTrue();
        assertThat(message.isAddressedTo(User.builder()
                .id(UUID.randomUUID())
                .email("other@example.com")
                .passwordHash("hash")
                .displayName("Other")
                .build())).isFalse();
    }

    private static Message messageWithStatus(MessageStatus status) {
        return Message.builder()
                .status(status)
                .title("Title")
                .content("Body")
                .recipientEmail("a@example.com")
                .recipientType(RecipientType.SELF)
                .unlockAt(Instant.parse("2030-01-01T00:00:00Z"))
                .build();
    }
}
