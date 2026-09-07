package com.futuremessage.service;

import com.futuremessage.config.MailProperties;
import com.futuremessage.config.NotificationSchedulerProperties;
import com.futuremessage.domain.Message;
import com.futuremessage.domain.MessageStatus;
import com.futuremessage.domain.NotificationStatus;
import com.futuremessage.domain.RecipientType;
import com.futuremessage.domain.User;
import com.futuremessage.mail.MailNotificationSender;
import com.futuremessage.mail.UnlockMailComposer;
import com.futuremessage.mail.UnlockMailContent;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-07T04:00:00Z");
    private static final Instant UNLOCK_AT = Instant.parse("2026-09-07T03:59:00Z");

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MailNotificationSender mailSender;

    private NotificationService notificationService;
    private User sender;
    private User recipient;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                messageRepository,
                new UnlockMailComposer(new MailProperties("noreply@test.local", "http://localhost:3000/inbox")),
                mailSender,
                Clock.fixed(NOW, ZoneOffset.UTC),
                new NotificationSchedulerProperties(true, Duration.ofSeconds(30), 50)
        );
        sender = user("11111111-1111-1111-1111-111111111111", "ada@example.com", "Ada");
        recipient = user("22222222-2222-2222-2222-222222222222", "bob@example.com", "Bob");
    }

    @Test
    void sendsPendingAvailableMessageAndMarksSent() {
        Message pending = availableMessage("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", NotificationStatus.PENDING);
        when(messageRepository.lockPendingNotificationMessageIds(50)).thenReturn(List.of(pending.getId()));
        when(messageRepository.findDetailedByIdIn(List.of(pending.getId()))).thenReturn(List.of(pending));

        int sent = notificationService.sendPendingNotifications();

        assertThat(sent).isEqualTo(1);
        assertThat(pending.getNotificationStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(pending.getNotifiedAt()).isEqualTo(NOW);
        verify(mailSender).send(eq("bob@example.com"), any(UnlockMailContent.class));
    }

    @Test
    void retriesFailedMessage() {
        Message failed = availableMessage("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", NotificationStatus.FAILED);
        when(messageRepository.lockPendingNotificationMessageIds(50)).thenReturn(List.of(failed.getId()));
        when(messageRepository.findDetailedByIdIn(List.of(failed.getId()))).thenReturn(List.of(failed));

        int sent = notificationService.sendPendingNotifications();

        assertThat(sent).isEqualTo(1);
        assertThat(failed.getNotificationStatus()).isEqualTo(NotificationStatus.SENT);
        verify(mailSender).send(eq("bob@example.com"), any(UnlockMailContent.class));
    }

    @Test
    void doesNotSendWhenAlreadySent() {
        Message alreadySent = availableMessage("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", NotificationStatus.SENT);
        alreadySent.setNotifiedAt(NOW.minusSeconds(60));
        when(messageRepository.lockPendingNotificationMessageIds(50)).thenReturn(List.of(alreadySent.getId()));
        when(messageRepository.findDetailedByIdIn(List.of(alreadySent.getId()))).thenReturn(List.of(alreadySent));

        int sent = notificationService.sendPendingNotifications();

        assertThat(sent).isZero();
        assertThat(alreadySent.getNotificationStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(alreadySent.getNotifiedAt()).isEqualTo(NOW.minusSeconds(60));
        verify(mailSender, never()).send(anyString(), any());
    }

    @Test
    void marksFailedAndContinuesWhenSmtpFails() {
        Message first = availableMessage("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", NotificationStatus.PENDING);
        Message second = availableMessage("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", NotificationStatus.PENDING);
        List<UUID> ids = List.of(first.getId(), second.getId());
        when(messageRepository.lockPendingNotificationMessageIds(50)).thenReturn(ids);
        when(messageRepository.findDetailedByIdIn(ids)).thenReturn(List.of(first, second));
        doThrow(new IllegalStateException("smtp down"))
                .doNothing()
                .when(mailSender).send(anyString(), any());

        int sent = notificationService.sendPendingNotifications();

        assertThat(sent).isEqualTo(1);
        assertThat(first.getNotificationStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(first.getNotifiedAt()).isNull();
        assertThat(second.getNotificationStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(second.getNotifiedAt()).isEqualTo(NOW);
    }

    @Test
    void returnsZeroWhenNothingPending() {
        when(messageRepository.lockPendingNotificationMessageIds(50)).thenReturn(List.of());

        int sent = notificationService.sendPendingNotifications();

        assertThat(sent).isZero();
        verify(messageRepository, never()).findDetailedByIdIn(any());
        verify(mailSender, never()).send(anyString(), any());
    }

    private Message availableMessage(String id, NotificationStatus notificationStatus) {
        return Message.builder()
                .id(UUID.fromString(id))
                .sender(sender)
                .recipientEmail(recipient.getEmail())
                .recipientUser(recipient)
                .recipientType(RecipientType.OTHER)
                .title("Title")
                .content("secret")
                .unlockAt(UNLOCK_AT)
                .status(MessageStatus.AVAILABLE)
                .notificationStatus(notificationStatus)
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
