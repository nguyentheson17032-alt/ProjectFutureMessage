package com.futuremessage.config;

import com.futuremessage.domain.Message;
import com.futuremessage.domain.NotificationStatus;
import com.futuremessage.domain.User;
import com.futuremessage.domain.UserRole;
import com.futuremessage.repository.MessageRepository;
import com.futuremessage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Local demo data for the admin UI: two users plus LOCKED / FAILED / OPENED messages.
 * Idempotent. Resets the FAILED demo letter on each startup so retry stays clickable.
 */
@Component
@Order(10)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.dev-seed", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(DevSeedProperties.class)
@Slf4j
public class DevDataSeeder implements ApplicationRunner {

    static final String ADA_EMAIL = "ada@futuremessage.local";
    static final String BOB_EMAIL = "bob@futuremessage.local";
    static final String DEMO_PASSWORD = "password1";
    static final String LOCKED_TITLE = "[dev] Thư còn khóa";
    static final String FAILED_TITLE = "[dev] Email gửi thất bại";
    static final String OPENED_TITLE = "[dev] Thư đã mở";

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Instant now = clock.instant();
        User ada = findOrCreateUser(ADA_EMAIL, "Ada", now);
        User bob = findOrCreateUser(BOB_EMAIL, "Bob", now);

        seedLocked(ada, bob, now);
        seedFailed(ada, bob, now);
        seedOpened(ada, bob, now);
        log.info(
                "Dev seed ready: {} / {} (password {}), titles [{}], [{}], [{}]",
                ADA_EMAIL,
                BOB_EMAIL,
                DEMO_PASSWORD,
                LOCKED_TITLE,
                FAILED_TITLE,
                OPENED_TITLE
        );
    }

    private User findOrCreateUser(String email, String displayName, Instant now) {
        String normalized = User.normalizeEmail(email);
        return userRepository.findByEmail(normalized).orElseGet(() -> userRepository.save(User.builder()
                .email(normalized)
                .passwordHash(passwordEncoder.encode(DEMO_PASSWORD))
                .displayName(displayName)
                .emailVerified(true)
                .role(UserRole.USER)
                .enabled(true)
                .createdAt(now)
                .updatedAt(now)
                .build()));
    }

    private void seedLocked(User ada, User bob, Instant now) {
        if (messageRepository.findFirstByTitle(LOCKED_TITLE).isPresent()) {
            return;
        }
        messageRepository.save(Message.compose(
                ada,
                LOCKED_TITLE,
                "Nội dung thư khóa — admin không được đọc cho đến khi mở khóa.",
                now.plus(Duration.ofDays(14)),
                bob.getEmail(),
                bob,
                now
        ));
    }

    private void seedFailed(User ada, User bob, Instant now) {
        messageRepository.findFirstByTitle(FAILED_TITLE).ifPresentOrElse(existing -> {
            if (existing.getNotificationStatus() != NotificationStatus.FAILED) {
                // Demo-only: keep the retry button available after the local job sends mail.
                existing.setNotificationStatus(NotificationStatus.FAILED);
                log.info("Reset demo notification to FAILED for {}", FAILED_TITLE);
            }
        }, () -> {
            Message failed = Message.compose(
                    ada,
                    FAILED_TITLE,
                    "Nội dung thư demo — admin được xem vì đã AVAILABLE. Bấm Gửi lại email trên UI.",
                    now.plus(Duration.ofHours(2)),
                    bob.getEmail(),
                    bob,
                    now
            );
            failed.setUnlockAt(now.minus(Duration.ofHours(1)));
            failed.markAvailable(now);
            failed.markNotificationFailed();
            messageRepository.save(failed);
        });
    }

    private void seedOpened(User ada, User bob, Instant now) {
        if (messageRepository.findFirstByTitle(OPENED_TITLE).isPresent()) {
            return;
        }
        Message opened = Message.compose(
                ada,
                OPENED_TITLE,
                "Thư đã mở — dùng để xem content trên trang quản trị.",
                now.plus(Duration.ofHours(2)),
                bob.getEmail(),
                bob,
                now
        );
        opened.setUnlockAt(now.minus(Duration.ofDays(1)));
        opened.markAvailable(now);
        opened.open(now);
        opened.markNotificationSent(now);
        messageRepository.save(opened);
    }
}
