package com.futuremessage.domain;

import com.futuremessage.common.BusinessException;
import com.futuremessage.common.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageTest {

    private static final Instant NOW = Instant.parse("2026-09-07T03:00:00Z");
    private static final Instant FUTURE = Instant.parse("2030-01-01T00:00:00Z");

    private final User sender = user("11111111-1111-1111-1111-111111111111", "ada@example.com", "Ada");
    private final User recipient = user("22222222-2222-2222-2222-222222222222", "bob@example.com", "Bob");

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
        User recipientUser = User.builder()
                .id(UUID.randomUUID())
                .email("recipient@example.com")
                .passwordHash("hash")
                .displayName("Recipient")
                .build();
        Message message = Message.builder()
                .status(MessageStatus.LOCKED)
                .recipientEmail("recipient@example.com")
                .recipientUser(recipientUser)
                .build();

        assertThat(message.isAddressedTo(recipientUser)).isTrue();
        assertThat(message.isAddressedTo(User.builder()
                .id(UUID.randomUUID())
                .email("other@example.com")
                .passwordHash("hash")
                .displayName("Other")
                .build())).isFalse();
    }

    @Test
    void composeSelfMessageWhenRecipientEmailOmitted() {
        Message message = Message.compose(sender, "Hello", "Be kind", FUTURE, null, sender, NOW);

        assertThat(message.getRecipientType()).isEqualTo(RecipientType.SELF);
        assertThat(message.getRecipientEmail()).isEqualTo("ada@example.com");
        assertThat(message.getRecipientUser()).isEqualTo(sender);
        assertThat(message.getStatus()).isEqualTo(MessageStatus.LOCKED);
        assertThat(message.getNotificationStatus()).isEqualTo(NotificationStatus.PENDING);
    }

    @Test
    void composeRejectsUnlockAtThatIsNotInTheFuture() {
        assertThatThrownBy(() -> Message.compose(sender, "Title", "Body", NOW, null, sender, NOW))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCode.UNLOCK_AT_MUST_BE_FUTURE);
    }

    @Test
    void applyEditUpdatesLockedMessageAndRejectsPastUnlockAt() {
        Message message = lockedOtherMessage();

        message.applyEdit("New title", "New body", FUTURE.plusSeconds(60), NOW);

        assertThat(message.getTitle()).isEqualTo("New title");
        assertThat(message.getContent()).isEqualTo("New body");
        assertThat(message.getUnlockAt()).isEqualTo(FUTURE.plusSeconds(60));

        assertThatThrownBy(() -> message.applyEdit(null, null, NOW.minusSeconds(1), NOW))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCode.UNLOCK_AT_MUST_BE_FUTURE);
    }

    @Test
    void applyEditRejectedWhenNotLocked() {
        Message message = lockedOtherMessage();
        message.setStatus(MessageStatus.AVAILABLE);

        assertThatThrownBy(() -> message.applyEdit("Nope", null, null, NOW))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCode.MESSAGE_NOT_EDITABLE);
    }

    @Test
    void cancelOnlyWhileLocked() {
        Message message = lockedOtherMessage();
        message.cancel();
        assertThat(message.getStatus()).isEqualTo(MessageStatus.CANCELLED);

        Message available = lockedOtherMessage();
        available.setStatus(MessageStatus.AVAILABLE);
        assertThatThrownBy(available::cancel)
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCode.MESSAGE_NOT_EDITABLE);
    }

    @Test
    void openSetsOpenedAtOnceAndDoesNotOverwrite() {
        Message message = lockedOtherMessage();
        message.setStatus(MessageStatus.AVAILABLE);

        message.open(NOW);
        assertThat(message.getStatus()).isEqualTo(MessageStatus.OPENED);
        assertThat(message.getOpenedAt()).isEqualTo(NOW);

        message.open(NOW.plusSeconds(30));
        message.setOpenedAt(NOW.plusSeconds(90));
        assertThat(message.getOpenedAt()).isEqualTo(NOW);
    }

    @Test
    void openRejectsLockedAndCancelled() {
        Message locked = lockedOtherMessage();
        assertThatThrownBy(() -> locked.open(NOW))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCode.MESSAGE_LOCKED);

        Message cancelled = lockedOtherMessage();
        cancelled.cancel();
        assertThatThrownBy(() -> cancelled.open(NOW))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCode.MESSAGE_NOT_AVAILABLE);
    }

    @Test
    void markAvailableWhenUnlockTimeReached() {
        Message message = lockedOtherMessage();

        assertThatThrownBy(() -> message.markAvailable(FUTURE.minusSeconds(1)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCode.MESSAGE_LOCKED);

        message.markAvailable(FUTURE);
        assertThat(message.getStatus()).isEqualTo(MessageStatus.AVAILABLE);

        message.markAvailable(FUTURE.plusSeconds(1));
        assertThat(message.getStatus()).isEqualTo(MessageStatus.AVAILABLE);
    }

    @Test
    void claimRecipientOnlyWhenUnclaimedAndEmailMatches() {
        Message unclaimed = Message.builder()
                .sender(sender)
                .recipientEmail("bob@example.com")
                .recipientType(RecipientType.OTHER)
                .title("Title")
                .content("secret")
                .unlockAt(FUTURE)
                .status(MessageStatus.LOCKED)
                .build();

        assertThat(unclaimed.claimRecipient(recipient)).isTrue();
        assertThat(unclaimed.getRecipientUser()).isEqualTo(recipient);

        User otherBob = user("44444444-4444-4444-4444-444444444444", "bob@example.com", "Other Bob");
        assertThat(unclaimed.claimRecipient(otherBob)).isFalse();
        assertThat(unclaimed.getRecipientUser()).isEqualTo(recipient);

        Message mismatched = lockedOtherMessage();
        mismatched.setRecipientUser(null);
        User eve = user("33333333-3333-3333-3333-333333333333", "eve@example.com", "Eve");
        assertThat(mismatched.claimRecipient(eve)).isFalse();
        assertThat(mismatched.getRecipientUser()).isNull();
    }

    private Message lockedOtherMessage() {
        return Message.builder()
                .id(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
                .sender(sender)
                .recipientEmail(recipient.getEmail())
                .recipientUser(recipient)
                .recipientType(RecipientType.OTHER)
                .title("Title")
                .content("secret")
                .unlockAt(FUTURE)
                .status(MessageStatus.LOCKED)
                .notificationStatus(NotificationStatus.PENDING)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
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

    private static User user(String id, String email, String name) {
        return User.builder()
                .id(UUID.fromString(id))
                .email(email)
                .passwordHash("hash")
                .displayName(name)
                .build();
    }
}
