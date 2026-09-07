package com.futuremessage.service;

import com.futuremessage.common.BusinessException;
import com.futuremessage.common.ErrorCode;
import com.futuremessage.domain.Message;
import com.futuremessage.domain.MessageStatus;
import com.futuremessage.domain.NotificationStatus;
import com.futuremessage.domain.RecipientType;
import com.futuremessage.domain.User;
import com.futuremessage.repository.MessageRepository;
import com.futuremessage.repository.UserRepository;
import com.futuremessage.web.dto.CreateMessageRequest;
import com.futuremessage.web.dto.MessageResponse;
import com.futuremessage.web.dto.UpdateMessageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-07T01:00:00Z");
    private static final Instant FUTURE = Instant.parse("2030-01-01T00:00:00Z");

    @Mock
    private MessageRepository messageRepository;
    @Mock
    private UserRepository userRepository;

    private MessageService messageService;
    private User sender;
    private User recipient;

    @BeforeEach
    void setUp() {
        messageService = new MessageService(
                messageRepository,
                userRepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        sender = user("11111111-1111-1111-1111-111111111111", "ada@example.com", "Ada");
        recipient = user("22222222-2222-2222-2222-222222222222", "bob@example.com", "Bob");
    }

    @Test
    void createSelfMessageWhenRecipientEmailOmitted() {
        when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(sender));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message message = invocation.getArgument(0);
            message.setId(UUID.randomUUID());
            message.setCreatedAt(NOW);
            message.setUpdatedAt(NOW);
            return message;
        });

        MessageResponse response = messageService.create(
                sender.getId(),
                new CreateMessageRequest("Hello future", "Be kind", FUTURE, null)
        );

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());
        Message saved = captor.getValue();
        assertThat(saved.getRecipientType()).isEqualTo(RecipientType.SELF);
        assertThat(saved.getRecipientEmail()).isEqualTo("ada@example.com");
        assertThat(saved.getRecipientUser()).isEqualTo(sender);
        assertThat(saved.getStatus()).isEqualTo(MessageStatus.LOCKED);
        assertThat(saved.getNotificationStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(response.content()).isEqualTo("Be kind");
        assertThat(response.recipientType()).isEqualTo(RecipientType.SELF);
    }

    @Test
    void createOtherMessageLinksExistingRecipient() {
        when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));
        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(recipient));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> withTimestamps(invocation.getArgument(0)));

        MessageResponse response = messageService.create(
                sender.getId(),
                new CreateMessageRequest("For Bob", "A gift", FUTURE, "  Bob@Example.COM ")
        );

        assertThat(response.recipientType()).isEqualTo(RecipientType.OTHER);
        assertThat(response.recipientEmail()).isEqualTo("bob@example.com");
        assertThat(response.recipientUserId()).isEqualTo(recipient.getId());
    }

    @Test
    void createRejectsUnlockAtInThePast() {
        when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));

        assertThatThrownBy(() -> messageService.create(
                sender.getId(),
                new CreateMessageRequest("Title", "Body", NOW.minusSeconds(1), null)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCode.UNLOCK_AT_MUST_BE_FUTURE);
    }

    @Test
    void getHidesLockedContentFromRecipientButNotSender() {
        Message message = lockedOtherMessage();
        when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));
        when(userRepository.findById(recipient.getId())).thenReturn(Optional.of(recipient));
        when(messageRepository.findDetailedById(message.getId())).thenReturn(Optional.of(message));

        assertThat(messageService.get(sender.getId(), message.getId()).content()).isEqualTo("secret");
        assertThat(messageService.get(recipient.getId(), message.getId()).content()).isNull();
    }

    @Test
    void getUnknownViewerReturnsNotFound() {
        User stranger = user("33333333-3333-3333-3333-333333333333", "eve@example.com", "Eve");
        Message message = lockedOtherMessage();
        when(userRepository.findById(stranger.getId())).thenReturn(Optional.of(stranger));
        when(messageRepository.findDetailedById(message.getId())).thenReturn(Optional.of(message));

        assertThatThrownBy(() -> messageService.get(stranger.getId(), message.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCode.MESSAGE_NOT_FOUND);
    }

    @Test
    void updateAllowsSenderWhileLocked() {
        Message message = lockedOtherMessage();
        when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));
        when(messageRepository.findDetailedById(message.getId())).thenReturn(Optional.of(message));

        MessageResponse response = messageService.update(
                sender.getId(),
                message.getId(),
                new UpdateMessageRequest("New title", "New body", FUTURE.plusSeconds(60))
        );

        assertThat(message.getTitle()).isEqualTo("New title");
        assertThat(message.getContent()).isEqualTo("New body");
        assertThat(response.title()).isEqualTo("New title");
    }

    @Test
    void updateRejectsWhenNotLocked() {
        Message message = lockedOtherMessage();
        message.setStatus(MessageStatus.AVAILABLE);
        when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));
        when(messageRepository.findDetailedById(message.getId())).thenReturn(Optional.of(message));

        assertThatThrownBy(() -> messageService.update(
                sender.getId(),
                message.getId(),
                new UpdateMessageRequest("Nope", null, null)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCode.MESSAGE_NOT_EDITABLE);
    }

    @Test
    void recipientCannotUpdate() {
        Message message = lockedOtherMessage();
        when(userRepository.findById(recipient.getId())).thenReturn(Optional.of(recipient));
        when(messageRepository.findDetailedById(message.getId())).thenReturn(Optional.of(message));

        assertThatThrownBy(() -> messageService.update(
                recipient.getId(),
                message.getId(),
                new UpdateMessageRequest("Hacked", null, null)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCode.NOT_SENDER);
    }

    @Test
    void cancelSetsCancelledWhenLocked() {
        Message message = lockedOtherMessage();
        when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));
        when(messageRepository.findDetailedById(message.getId())).thenReturn(Optional.of(message));

        messageService.cancel(sender.getId(), message.getId());

        assertThat(message.getStatus()).isEqualTo(MessageStatus.CANCELLED);
    }

    @Test
    void openSetsOpenedAtOnceAndIsIdempotent() {
        Message message = lockedOtherMessage();
        message.setStatus(MessageStatus.AVAILABLE);
        when(userRepository.findById(recipient.getId())).thenReturn(Optional.of(recipient));
        when(messageRepository.findDetailedById(message.getId())).thenReturn(Optional.of(message));

        MessageResponse first = messageService.open(recipient.getId(), message.getId());
        Instant openedAt = first.openedAt();
        MessageResponse second = messageService.open(recipient.getId(), message.getId());

        assertThat(first.status()).isEqualTo(MessageStatus.OPENED);
        assertThat(openedAt).isEqualTo(NOW);
        assertThat(second.openedAt()).isEqualTo(openedAt);
        assertThat(message.getOpenedAt()).isEqualTo(NOW);
    }

    @Test
    void openRejectsLockedMessage() {
        Message message = lockedOtherMessage();
        when(userRepository.findById(recipient.getId())).thenReturn(Optional.of(recipient));
        when(messageRepository.findDetailedById(message.getId())).thenReturn(Optional.of(message));

        assertThatThrownBy(() -> messageService.open(recipient.getId(), message.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCode.MESSAGE_LOCKED);
    }

    @Test
    void senderOfOtherMessageCannotOpen() {
        Message message = lockedOtherMessage();
        message.setStatus(MessageStatus.AVAILABLE);
        when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));
        when(messageRepository.findDetailedById(message.getId())).thenReturn(Optional.of(message));

        assertThatThrownBy(() -> messageService.open(sender.getId(), message.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCode.NOT_RECIPIENT);
    }

    @Test
    void inboxHidesLockedContent() {
        Message message = lockedOtherMessage();
        when(userRepository.findById(recipient.getId())).thenReturn(Optional.of(recipient));
        when(messageRepository.findInbox(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(message)));

        var page = messageService.listInbox(recipient.getId(), 0, 20);

        assertThat(page.items()).hasSize(1);
        assertThat(page.items().getFirst().content()).isNull();
        assertThat(page.items().getFirst().title()).isEqualTo("Title");
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

    private Message withTimestamps(Message message) {
        message.setId(UUID.randomUUID());
        message.setCreatedAt(NOW);
        message.setUpdatedAt(NOW);
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
