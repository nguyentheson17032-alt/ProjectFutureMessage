package com.futuremessage.service;

import com.futuremessage.config.UnlockSchedulerProperties;
import com.futuremessage.domain.Message;
import com.futuremessage.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Use case unlock: tìm message {@code LOCKED} đã đến hạn, chuyển sang {@code AVAILABLE}.
 * <p>
 * Không gửi email ở bước này. {@code notificationStatus} giữ {@code PENDING} để bước Email
 * gửi thông báo (và retry khi {@code FAILED}). Không ghi đè nếu đã {@code SENT}.
 */
@Service
public class UnlockService {

    private static final Logger log = LoggerFactory.getLogger(UnlockService.class);

    private final MessageRepository messageRepository;
    private final Clock clock;
    private final UnlockSchedulerProperties properties;

    public UnlockService(
            MessageRepository messageRepository,
            Clock clock,
            UnlockSchedulerProperties properties
    ) {
        this.messageRepository = messageRepository;
        this.clock = clock;
        this.properties = properties;
    }

    /**
     * Khóa một batch hàng đến hạn, gọi {@link Message#markAvailable(Instant)}, trả về số message đã unlock.
     * Cùng một transaction với {@code FOR UPDATE SKIP LOCKED}: instance khác sẽ bỏ qua hàng đang xử lý.
     */
    @Transactional
    public int unlockDueMessages() {
        Instant now = clock.instant();
        List<UUID> ids = messageRepository.lockDueLockedMessageIds(now, properties.batchSize());
        if (ids.isEmpty()) {
            return 0;
        }

        List<Message> messages = messageRepository.findAllById(ids);
        List<UUID> unlockedIds = new ArrayList<>(messages.size());
        for (Message message : messages) {
            message.markAvailable(now);
            unlockedIds.add(message.getId());
        }

        log.info("Unlocked {} message(s): {}", unlockedIds.size(), unlockedIds);
        return unlockedIds.size();
    }
}
