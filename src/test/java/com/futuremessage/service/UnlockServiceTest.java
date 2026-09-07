package com.futuremessage.service;

import com.futuremessage.common.BusinessException;
import com.futuremessage.common.ErrorCode;
import com.futuremessage.config.UnlockSchedulerProperties;
import com.futuremessage.domain.Message;
import com.futuremessage.domain.MessageStatus;
import com.futuremessage.domain.NotificationStatus;
import com.futuremessage.domain.RecipientType;
import com.futuremessage.domain.User;
import com.futuremessage.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnlockServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-07T04:00:00Z");
    private static final Instant UNLOCK_AT = Instant.parse("2026-09-07T03:59:00Z");
    private static final Instant STILL_FUTURE = Instant.parse("2030-01-01T00:00:00Z");

    @Mock
    private MessageRepository messageRepository;

    private UnlockService unlockService;
    private User sender;
    private User recipient;

    @BeforeEach
    void setUp() {
        unlockService = new UnlockService(
                messageRepository,
                Clock.fixed(NOW, ZoneOffset.UTC),
                new UnlockSchedulerProperties(true, Duration.ofSeconds(30), 50)
        );
        sender = user("11111111-1111-1111-1111-111111111111", "ada@example.com", "Ada");
        recipient = user("22222222-2222-2222-2222-222222222222", "bob@example.com", "Bob");
    }

    @Test
    void unlocksDueLockedMessagesAndLeavesNotificationPending() {
        Message due = lockedMessage("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", UNLOCK_AT);
        when(messageRepository.lockDueLockedMessageIds(NOW, 50)).thenReturn(List.of(due.getId()));
        when(messageRepository.findAllById(List.of(due.getId()))).thenReturn(List.of(due));

        int unlocked = unlockService.unlockDueMessages();

        assertThat(unlocked).isEqualTo(1);
        assertThat(due.getStatus()).isEqualTo(MessageStatus.AVAILABLE);
        assertThat(due.getNotificationStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(due.getOpenedAt()).isNull();
    }

    @Test
    void returnsZeroWhenNoDueMessages() {
        when(messageRepository.lockDueLockedMessageIds(NOW, 50)).thenReturn(List.of());

        int unlocked = unlockService.unlockDueMessages();

        assertThat(unlocked).isZero();
        verify(messageRepository, never()).findAllById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void unlocksABatchOfDueMessages() {
        Message first = lockedMessage("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", UNLOCK_AT);
        Message second = lockedMessage("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", UNLOCK_AT.minusSeconds(30));
        List<UUID> ids = List.of(first.getId(), second.getId());
        when(messageRepository.lockDueLockedMessageIds(NOW, 50)).thenReturn(ids);
        when(messageRepository.findAllById(ids)).thenReturn(List.of(first, second));

        int unlocked = unlockService.unlockDueMessages();

        assertThat(unlocked).isEqualTo(2);
        assertThat(first.getStatus()).isEqualTo(MessageStatus.AVAILABLE);
        assertThat(second.getStatus()).isEqualTo(MessageStatus.AVAILABLE);
    }

    @Test
    void doesNotUnlockWhenRepositoryReturnsALockedMessageStillInTheFuture() {
        Message notYetDue = lockedMessage("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", STILL_FUTURE);
        when(messageRepository.lockDueLockedMessageIds(NOW, 50)).thenReturn(List.of(notYetDue.getId()));
        when(messageRepository.findAllById(List.of(notYetDue.getId()))).thenReturn(List.of(notYetDue));

        assertThatThrownBy(() -> unlockService.unlockDueMessages())
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCode.MESSAGE_LOCKED);
        assertThat(notYetDue.getStatus()).isEqualTo(MessageStatus.LOCKED);
    }

    private Message lockedMessage(String id, Instant unlockAt) {
        return Message.builder()
                .id(UUID.fromString(id))
                .sender(sender)
                .recipientEmail(recipient.getEmail())
                .recipientUser(recipient)
                .recipientType(RecipientType.OTHER)
                .title("Title")
                .content("secret")
                .unlockAt(unlockAt)
                .status(MessageStatus.LOCKED)
                .notificationStatus(NotificationStatus.PENDING)
                .createdAt(NOW.minusSeconds(3600))
                .updatedAt(NOW.minusSeconds(3600))
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
