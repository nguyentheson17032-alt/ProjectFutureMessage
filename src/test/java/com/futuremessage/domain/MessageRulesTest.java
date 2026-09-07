package com.futuremessage.domain;

import com.futuremessage.common.BusinessException;
import com.futuremessage.common.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageRulesTest {

    private static final Instant NOW = Instant.parse("2026-09-07T03:00:00Z");
    private static final Instant FUTURE = Instant.parse("2030-01-01T00:00:00Z");

    private final User sender = user("11111111-1111-1111-1111-111111111111", "ada@example.com", "Ada");
    private final User recipient = user("22222222-2222-2222-2222-222222222222", "bob@example.com", "Bob");
    private final User stranger = user("33333333-3333-3333-3333-333333333333", "eve@example.com", "Eve");

    @Test
    void unlockAtMustBeStrictlyInTheFuture() {
        assertThat(MessageRules.requireFutureUnlockAt(FUTURE, NOW)).isEqualTo(FUTURE);

        assertThatThrownBy(() -> MessageRules.requireFutureUnlockAt(NOW, NOW))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCode.UNLOCK_AT_MUST_BE_FUTURE);
        assertThatThrownBy(() -> MessageRules.requireFutureUnlockAt(NOW.minusSeconds(1), NOW))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCode.UNLOCK_AT_MUST_BE_FUTURE);
    }

    @Test
    void onlySenderMayEditOrCancel() {
        Message message = lockedOtherMessage();

        MessageRules.requireSender(message, sender);
        assertThatThrownBy(() -> MessageRules.requireSender(message, recipient))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCode.NOT_SENDER);
    }

    @Test
    void onlyRecipientMayOpen() {
        Message message = lockedOtherMessage();

        MessageRules.requireRecipient(message, recipient);
        assertThatThrownBy(() -> MessageRules.requireRecipient(message, sender))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCode.NOT_RECIPIENT);
    }

    @Test
    void strangerIsHiddenAsNotFound() {
        Message message = lockedOtherMessage();

        MessageRules.requireParticipant(message, sender);
        MessageRules.requireParticipant(message, recipient);
        assertThatThrownBy(() -> MessageRules.requireParticipant(message, stranger))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCode.MESSAGE_NOT_FOUND);
    }

    @Test
    void omittedRecipientEmailMeansSelf() {
        assertThat(MessageRules.resolveRecipientEmail(sender, null)).isEqualTo("ada@example.com");
        assertThat(MessageRules.resolveRecipientEmail(sender, "  ")).isEqualTo("ada@example.com");
        assertThat(MessageRules.resolveRecipientEmail(sender, "  Bob@Example.COM "))
                .isEqualTo("bob@example.com");
        assertThat(MessageRules.recipientType(sender, "ada@example.com")).isEqualTo(RecipientType.SELF);
        assertThat(MessageRules.recipientType(sender, "bob@example.com")).isEqualTo(RecipientType.OTHER);
    }

    @Test
    void senderSeesLockedContentRecipientDoesNot() {
        Message message = lockedOtherMessage();

        assertThat(MessageRules.visibleContent(message, sender)).isEqualTo("secret");
        assertThat(MessageRules.visibleContent(message, recipient)).isNull();
        assertThat(MessageRules.visibleContent(message, stranger)).isNull();

        message.setStatus(MessageStatus.AVAILABLE);
        assertThat(MessageRules.visibleContent(message, recipient)).isEqualTo("secret");
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
