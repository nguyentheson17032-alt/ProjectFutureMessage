package com.futuremessage.service;

import com.futuremessage.config.NotificationSchedulerProperties;
import com.futuremessage.domain.Message;
import com.futuremessage.mail.MailNotificationSender;
import com.futuremessage.mail.UnlockMailComposer;
import com.futuremessage.mail.UnlockMailContent;
import com.futuremessage.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Use case email: tìm message đã {@code AVAILABLE}/{@code OPENED} còn {@code PENDING} hoặc {@code FAILED},
 * gửi thông báo, đánh {@code SENT} hoặc {@code FAILED}. Không gửi lại nếu đã {@code SENT}.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final MessageRepository messageRepository;
    private final UnlockMailComposer mailComposer;
    private final MailNotificationSender mailSender;
    private final Clock clock;
    private final NotificationSchedulerProperties properties;

    public NotificationService(
            MessageRepository messageRepository,
            UnlockMailComposer mailComposer,
            MailNotificationSender mailSender,
            Clock clock,
            NotificationSchedulerProperties properties
    ) {
        this.messageRepository = messageRepository;
        this.mailComposer = mailComposer;
        this.mailSender = mailSender;
        this.clock = clock;
        this.properties = properties;
    }

    /**
     * Khóa một batch hàng cần gửi, gửi từng email, cập nhật {@code notificationStatus}.
     * Một email fail không làm fail cả batch — message đó {@code FAILED} để lần sau retry.
     */
    @Transactional
    public int sendPendingNotifications() {
        List<UUID> ids = messageRepository.lockPendingNotificationMessageIds(properties.batchSize());
        if (ids.isEmpty()) {
            return 0;
        }

        List<Message> messages = messageRepository.findDetailedByIdIn(ids);
        Instant now = clock.instant();
        int sent = 0;
        for (Message message : messages) {
            if (!message.canNotify()) {
                continue;
            }
            try {
                UnlockMailContent content = mailComposer.compose(message);
                mailSender.send(message.getRecipientEmail(), content);
                message.markNotificationSent(now);
                sent++;
                log.info("Sent unlock notification for message {} to {}", message.getId(), message.getRecipientEmail());
            } catch (RuntimeException ex) {
                message.markNotificationFailed();
                log.warn(
                        "Unlock notification failed for message {} to {}; will retry",
                        message.getId(),
                        message.getRecipientEmail(),
                        ex
                );
            }
        }
        return sent;
    }
}
