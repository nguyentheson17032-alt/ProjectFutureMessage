package com.futuremessage.config;

import com.futuremessage.domain.User;
import com.futuremessage.domain.UserRole;
import com.futuremessage.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void createsAdminWhenEmailIsUnknown() {
        AdminBootstrap bootstrap = new AdminBootstrap(
                new AdminBootstrapProperties("admin@example.com", "adminpass1", "Admin"),
                userRepository,
                passwordEncoder
        );
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("adminpass1")).thenReturn("hashed");

        bootstrap.run(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("admin@example.com");
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(captor.getValue().isEnabled()).isTrue();
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed");
    }

    @Test
    void doesNotPromoteExistingUser() {
        AdminBootstrap bootstrap = new AdminBootstrap(
                new AdminBootstrapProperties("ada@example.com", "adminpass1", "Admin"),
                userRepository,
                passwordEncoder
        );
        User existing = User.builder().email("ada@example.com").role(UserRole.USER).build();
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(existing));

        bootstrap.run(null);

        verify(userRepository, never()).save(any());
        assertThat(existing.getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    void skipsWhenNotConfigured() {
        AdminBootstrap bootstrap = new AdminBootstrap(
                new AdminBootstrapProperties("", "", "Admin"),
                userRepository,
                passwordEncoder
        );

        bootstrap.run(null);

        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any());
    }
}
