package com.futuremessage.service;

import com.futuremessage.common.BusinessException;
import com.futuremessage.common.ErrorCode;
import com.futuremessage.common.PageResponse;
import com.futuremessage.domain.Message;
import com.futuremessage.domain.MessageStatus;
import com.futuremessage.domain.NotificationStatus;
import com.futuremessage.domain.User;
import com.futuremessage.domain.UserRole;
import com.futuremessage.repository.MessageRepository;
import com.futuremessage.repository.UserRepository;
import com.futuremessage.web.AdminMapper;
import com.futuremessage.web.dto.AdminMessageResponse;
import com.futuremessage.web.dto.AdminMessageSummaryResponse;
import com.futuremessage.web.dto.AdminStatsResponse;
import com.futuremessage.web.dto.AdminUserDetailResponse;
import com.futuremessage.web.dto.UserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);
    static final int MAX_PAGE_SIZE = 100;
    private static final Duration UNLOCK_WINDOW = Duration.ofHours(24);

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final Clock clock;

    public AdminService(UserRepository userRepository, MessageRepository messageRepository, Clock clock) {
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AdminStatsResponse stats() {
        Instant now = clock.instant();
        return new AdminStatsResponse(
                userRepository.count(),
                new AdminStatsResponse.MessagesByStatus(
                        messageRepository.countByStatus(MessageStatus.LOCKED),
                        messageRepository.countByStatus(MessageStatus.AVAILABLE),
                        messageRepository.countByStatus(MessageStatus.OPENED),
                        messageRepository.countByStatus(MessageStatus.CANCELLED)
                ),
                new AdminStatsResponse.NotificationCounts(
                        messageRepository.countByNotificationStatus(NotificationStatus.PENDING),
                        messageRepository.countByNotificationStatus(NotificationStatus.SENT),
                        messageRepository.countByNotificationStatus(NotificationStatus.FAILED)
                ),
                messageRepository.countLockedUnlockingBetween(now, now.plus(UNLOCK_WINDOW))
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listUsers(String q, Boolean enabled, UserRole role, int page, int size) {
        String query = q == null ? "" : q.trim();
        return PageResponse.from(
                userRepository.search(query, enabled, role, pageRequest(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))),
                UserResponse::from
        );
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUser(UUID userId) {
        User user = requireUser(userId);
        long sentCount = messageRepository.countBySender_Id(user.getId());
        long inboxCount = messageRepository.countInbox(user.getId(), user.getEmail());
        return AdminUserDetailResponse.from(user, sentCount, inboxCount);
    }

    @Transactional
    public AdminUserDetailResponse updateUserEnabled(UUID userId, boolean enabled) {
        User user = requireUser(userId);
        user.setEnabled(enabled);
        userRepository.saveAndFlush(user);
        log.info("Admin set user {} enabled={}", user.getId(), enabled);
        return AdminUserDetailResponse.from(
                user,
                messageRepository.countBySender_Id(user.getId()),
                messageRepository.countInbox(user.getId(), user.getEmail())
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminMessageSummaryResponse> listMessages(
            MessageStatus status,
            NotificationStatus notificationStatus,
            String senderEmail,
            String recipientEmail,
            int page,
            int size
    ) {
        return PageResponse.from(
                messageRepository.searchForAdmin(
                        status,
                        notificationStatus,
                        blankToNormalizedEmail(senderEmail),
                        blankToNormalizedEmail(recipientEmail),
                        pageRequest(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
                ),
                AdminMapper::toSummary
        );
    }

    @Transactional(readOnly = true)
    public AdminMessageResponse getMessage(UUID messageId) {
        return AdminMapper.toResponse(requireMessage(messageId));
    }

    @Transactional
    public AdminMessageResponse retryNotification(UUID messageId) {
        Message message = requireMessage(messageId);
        message.queueNotificationRetry();
        log.info("Admin queued notification retry for message {}", message.getId());
        return AdminMapper.toResponse(message);
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    private Message requireMessage(UUID messageId) {
        return messageRepository.findDetailedById(messageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_NOT_FOUND));
    }

    private static String blankToNormalizedEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return User.normalizeEmail(email);
    }

    private static PageRequest pageRequest(int page, int size, Sort sort) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "page must be >= 0 and size must be 1-100");
        }
        return PageRequest.of(page, size, sort);
    }
}
