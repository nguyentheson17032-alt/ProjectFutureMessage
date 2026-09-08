package com.futuremessage.config;

import com.futuremessage.domain.User;
import com.futuremessage.domain.UserRole;
import com.futuremessage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tạo ADMIN lần đầu nếu email chưa tồn tại. Không tự phong user thường thành admin,
 * không ghi đè mật khẩu admin đã có.
 */
@Component
@Order(0)
@RequiredArgsConstructor
@EnableConfigurationProperties(AdminBootstrapProperties.class)
@Slf4j
public class AdminBootstrap implements ApplicationRunner {

    private final AdminBootstrapProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.configured()) {
            log.info("Admin bootstrap skipped (ADMIN_EMAIL / ADMIN_PASSWORD not set)");
            return;
        }

        String email = User.normalizeEmail(properties.email());
        userRepository.findByEmail(email).ifPresentOrElse(existing -> {
            if (existing.getRole() != UserRole.ADMIN) {
                log.warn("Admin bootstrap skipped: {} already exists as {}", email, existing.getRole());
            } else {
                log.info("Admin account already exists: {}", email);
            }
        }, () -> {
            User admin = User.builder()
                    .email(email)
                    .passwordHash(passwordEncoder.encode(properties.password()))
                    .displayName(properties.displayName())
                    .emailVerified(true)
                    .role(UserRole.ADMIN)
                    .enabled(true)
                    .build();
            userRepository.save(admin);
            log.info("Bootstrapped admin account: {}", email);
        });
    }
}
