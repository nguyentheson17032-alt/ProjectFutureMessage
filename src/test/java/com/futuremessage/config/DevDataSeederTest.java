package com.futuremessage.config;

import com.futuremessage.domain.Message;
import com.futuremessage.domain.MessageStatus;
import com.futuremessage.domain.NotificationStatus;
import com.futuremessage.domain.User;
import com.futuremessage.domain.UserRole;
import com.futuremessage.repository.MessageRepository;
import com.futuremessage.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevDataSeederTest {

    private static final Instant NOW = Instant.parse("2026-09-08T12:00:00Z");

    @Mock
    private UserRepository userRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private DevDataSeeder seeder;
    private User ada;
    private User bob;

    @BeforeEach
    void setUp() {
        seeder = new DevDataSeeder(
                userRepository,
                messageRepository,
                passwordEncoder,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        ada = user("11111111-1111-1111-1111-111111111111", DevDataSeeder.ADA_EMAIL, "Ada");
        bob = user("22222222-2222-2222-2222-222222222222", DevDataSeeder.BOB_EMAIL, "Bob");
    }

    @Test
    void createsUsersAndThreeDemoMessages() {
        when(userRepository.findByEmail(DevDataSeeder.ADA_EMAIL)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(DevDataSeeder.BOB_EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(DevDataSeeder.DEMO_PASSWORD)).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(messageRepository.findFirstByTitle(any())).thenReturn(Optional.empty());
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        seeder.run(null);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository, times(3)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(Message::getTitle)
                .containsExactly(
                        DevDataSeeder.LOCKED_TITLE,
                        DevDataSeeder.FAILED_TITLE,
                        DevDataSeeder.OPENED_TITLE
                );
        Message locked = captor.getAllValues().get(0);
        Message failed = captor.getAllValues().get(1);
        Message opened = captor.getAllValues().get(2);
        assertThat(locked.getStatus()).isEqualTo(MessageStatus.LOCKED);
        assertThat(failed.getStatus()).isEqualTo(MessageStatus.AVAILABLE);
        assertThat(failed.getNotificationStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(opened.getStatus()).isEqualTo(MessageStatus.OPENED);
        assertThat(opened.getNotificationStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void isIdempotentAndResetsFailedDemoMail() {
        when(userRepository.findByEmail(DevDataSeeder.ADA_EMAIL)).thenReturn(Optional.of(ada));
        when(userRepository.findByEmail(DevDataSeeder.BOB_EMAIL)).thenReturn(Optional.of(bob));
        Message locked = Message.compose(ada, DevDataSeeder.LOCKED_TITLE, "x", NOW.plusSeconds(3600), bob.getEmail(), bob, NOW);
        Message failed = Message.compose(ada, DevDataSeeder.FAILED_TITLE, "x", NOW.plusSeconds(3600), bob.getEmail(), bob, NOW);
        failed.setUnlockAt(NOW.minusSeconds(60));
        failed.markAvailable(NOW);
        failed.markNotificationSent(NOW);
        Message opened = Message.compose(ada, DevDataSeeder.OPENED_TITLE, "x", NOW.plusSeconds(3600), bob.getEmail(), bob, NOW);
        when(messageRepository.findFirstByTitle(DevDataSeeder.LOCKED_TITLE)).thenReturn(Optional.of(locked));
        when(messageRepository.findFirstByTitle(DevDataSeeder.FAILED_TITLE)).thenReturn(Optional.of(failed));
        when(messageRepository.findFirstByTitle(DevDataSeeder.OPENED_TITLE)).thenReturn(Optional.of(opened));

        seeder.run(null);

        verify(messageRepository, never()).save(any());
        verify(userRepository, never()).save(any());
        assertThat(failed.getNotificationStatus()).isEqualTo(NotificationStatus.FAILED);
    }

    private static User user(String id, String email, String name) {
        return User.builder()
                .id(UUID.fromString(id))
                .email(email)
                .passwordHash("hashed")
                .displayName(name)
                .role(UserRole.USER)
                .enabled(true)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
    }
}
