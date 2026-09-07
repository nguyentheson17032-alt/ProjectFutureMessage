package com.futuremessage.service;

import com.futuremessage.common.BusinessException;
import com.futuremessage.common.ErrorCode;
import com.futuremessage.common.PageResponse;
import com.futuremessage.domain.Message;
import com.futuremessage.domain.MessageRules;
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
        String recipientEmail = MessageRules.resolveRecipientEmail(sender, request.recipientEmail());
        User recipientUser = userRepository.findByEmail(recipientEmail).orElse(null);

        Message message = Message.compose(
                sender,
                request.title(),
                request.content(),
                request.unlockAt(),
                recipientEmail,
                recipientUser,
                clock.instant()
        );

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
        MessageRules.requireSender(message, sender);
        message.applyEdit(request.title(), request.content(), request.unlockAt(), clock.instant());
        return MessageMapper.toResponse(message, sender);
    }

    @Transactional
    public void cancel(UUID senderId, UUID messageId) {
        User sender = requireUser(senderId);
        Message message = requireVisibleMessage(messageId, sender);
        MessageRules.requireSender(message, sender);
        message.cancel();
    }

    @Transactional
    public MessageResponse open(UUID recipientId, UUID messageId) {
        User recipient = requireUser(recipientId);
        Message message = requireVisibleMessage(messageId, recipient);
        MessageRules.requireRecipient(message, recipient);
        message.open(clock.instant().truncatedTo(ChronoUnit.MICROS));
        return MessageMapper.toResponse(message, recipient);
    }

    private Message requireVisibleMessage(UUID messageId, User viewer) {
        Message message = messageRepository.findDetailedById(messageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_NOT_FOUND));
        MessageRules.requireParticipant(message, viewer);
        return message;
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    private static PageRequest pageRequest(int page, int size, Sort sort) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "page must be >= 0 and size must be 1-100");
        }
        return PageRequest.of(page, size, sort);
    }
}
