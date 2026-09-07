package com.futuremessage.service;

import com.futuremessage.common.BusinessException;
import com.futuremessage.common.ErrorCode;
import com.futuremessage.common.PageResponse;
import com.futuremessage.domain.Message;
import com.futuremessage.domain.MessageStatus;
import com.futuremessage.domain.NotificationStatus;
import com.futuremessage.domain.RecipientType;
import com.futuremessage.domain.User;
import com.futuremessage.repository.MessageRepository;
import com.futuremessage.repository.UserRepository;
import com.futuremessage.web.MessageMapper;
import com.futuremessage.web.dto.CreateMessageRequest;
import com.futuremessage.web.dto.MessageResponse;
import com.futuremessage.web.dto.MessageSummaryResponse;
import com.futuremessage.web.dto.UpdateMessageRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class MessageService {

    static final int MAX_PAGE_SIZE = 100;

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public MessageService(MessageRepository messageRepository, UserRepository userRepository, Clock clock) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional
    public MessageResponse create(UUID senderId, CreateMessageRequest request) {
        User sender = requireUser(senderId);
        Instant unlockAt = requireFutureUnlockAt(request.unlockAt());

        String recipientEmail = resolveRecipientEmail(sender, request.recipientEmail());
        RecipientType recipientType = sender.hasEmail(recipientEmail) ? RecipientType.SELF : RecipientType.OTHER;
        User recipientUser = userRepository.findByEmail(recipientEmail).orElse(null);

        Message message = Message.builder()
                .sender(sender)
                .recipientEmail(recipientEmail)
                .recipientUser(recipientUser)
                .recipientType(recipientType)
                .title(request.title().trim())
                .content(request.content().trim())
                .unlockAt(unlockAt)
                .status(MessageStatus.LOCKED)
                .notificationStatus(NotificationStatus.PENDING)
                .build();

        return MessageMapper.toResponse(messageRepository.save(message), sender);
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageSummaryResponse> listSent(UUID senderId, int page, int size) {
        User sender = requireUser(senderId);
        return PageResponse.from(
                messageRepository.findBySender_Id(sender.getId(), pageRequest(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))),
                message -> MessageMapper.toSummary(message, sender)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageSummaryResponse> listInbox(UUID recipientId, int page, int size) {
        User recipient = requireUser(recipientId);
        return PageResponse.from(
                messageRepository.findInbox(
                        recipient.getId(),
                        recipient.getEmail(),
                        pageRequest(page, size, Sort.by(Sort.Direction.DESC, "unlockAt"))
                ),
                message -> MessageMapper.toSummary(message, recipient)
        );
    }

    @Transactional(readOnly = true)
    public MessageResponse get(UUID viewerId, UUID messageId) {
        User viewer = requireUser(viewerId);
        Message message = requireVisibleMessage(messageId, viewer);
        return MessageMapper.toResponse(message, viewer);
    }

    @Transactional
    public MessageResponse update(UUID senderId, UUID messageId, UpdateMessageRequest request) {
        User sender = requireUser(senderId);
        Message message = requireVisibleMessage(messageId, sender);
        if (!message.isSentBy(sender)) {
            throw new BusinessException(ErrorCode.NOT_SENDER);
        }
        if (!message.canEdit()) {
            throw new BusinessException(ErrorCode.MESSAGE_NOT_EDITABLE);
        }

        applyUpdate(message, request);
        return MessageMapper.toResponse(message, sender);
    }

    @Transactional
    public void cancel(UUID senderId, UUID messageId) {
        User sender = requireUser(senderId);
        Message message = requireVisibleMessage(messageId, sender);
        if (!message.isSentBy(sender)) {
            throw new BusinessException(ErrorCode.NOT_SENDER);
        }
        if (!message.canCancel()) {
            throw new BusinessException(ErrorCode.MESSAGE_NOT_EDITABLE);
        }
        message.setStatus(MessageStatus.CANCELLED);
    }

    @Transactional
    public MessageResponse open(UUID recipientId, UUID messageId) {
        User recipient = requireUser(recipientId);
        Message message = requireVisibleMessage(messageId, recipient);
        if (!message.isAddressedTo(recipient)) {
            throw new BusinessException(ErrorCode.NOT_RECIPIENT);
        }
        if (message.getStatus() == MessageStatus.OPENED) {
            return MessageMapper.toResponse(message, recipient);
        }
        if (message.getStatus() == MessageStatus.LOCKED) {
            throw new BusinessException(ErrorCode.MESSAGE_LOCKED);
        }
        if (!message.canOpen()) {
            throw new BusinessException(ErrorCode.MESSAGE_NOT_AVAILABLE);
        }

        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        message.setStatus(MessageStatus.OPENED);
        message.setOpenedAt(now);
        return MessageMapper.toResponse(message, recipient);
    }

    private void applyUpdate(Message message, UpdateMessageRequest request) {
        if (request.title() != null) {
            String title = request.title().trim();
            if (title.isEmpty()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "title must not be blank");
            }
            message.setTitle(title);
        }
        if (request.content() != null) {
            String content = request.content().trim();
            if (content.isEmpty()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "content must not be blank");
            }
            message.setContent(content);
        }
        if (request.unlockAt() != null) {
            message.setUnlockAt(requireFutureUnlockAt(request.unlockAt()));
        }
    }

    private Message requireVisibleMessage(UUID messageId, User viewer) {
        Message message = messageRepository.findDetailedById(messageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_NOT_FOUND));
        if (!message.isSentBy(viewer) && !message.isAddressedTo(viewer)) {
            throw new BusinessException(ErrorCode.MESSAGE_NOT_FOUND);
        }
        return message;
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    private Instant requireFutureUnlockAt(Instant unlockAt) {
        if (unlockAt == null || !unlockAt.isAfter(clock.instant())) {
            throw new BusinessException(ErrorCode.UNLOCK_AT_MUST_BE_FUTURE);
        }
        return unlockAt;
    }

    private static String resolveRecipientEmail(User sender, String recipientEmail) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            return sender.getEmail();
        }
        return User.normalizeEmail(recipientEmail);
    }

    private static PageRequest pageRequest(int page, int size, Sort sort) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "page must be >= 0 and size must be 1-100");
        }
        return PageRequest.of(page, size, sort);
    }
}
