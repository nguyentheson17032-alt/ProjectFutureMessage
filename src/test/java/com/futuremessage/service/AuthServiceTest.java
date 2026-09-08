package com.futuremessage.service;

import com.futuremessage.common.BusinessException;
import com.futuremessage.common.ErrorCode;
import com.futuremessage.config.JwtProperties;
import com.futuremessage.domain.RefreshToken;
import com.futuremessage.domain.User;
import com.futuremessage.domain.UserRole;
import com.futuremessage.repository.MessageRepository;
import com.futuremessage.repository.RefreshTokenRepository;
import com.futuremessage.repository.UserRepository;
import com.futuremessage.security.JwtService;
import com.futuremessage.security.TokenHasher;
import com.futuremessage.web.dto.AuthResponse;
import com.futuremessage.web.dto.LoginRequest;
import com.futuremessage.web.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-06T01:00:00Z");
    private static final JwtProperties JWT_PROPERTIES =
            new JwtProperties("t".repeat(32), Duration.ofMinutes(15), Duration.ofDays(7));

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                messageRepository,
                passwordEncoder,
                jwtService,
                JWT_PROPERTIES,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void registerCreatesUserNormalizesEmailAndIssuesTokens() {
        when(userRepository.existsByEmail("ada@example.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
            return user;
        });
        when(jwtService.createAccessToken(any(User.class))).thenReturn("access-token");
        when(jwtService.accessTokenTtl()).thenReturn(Duration.ofMinutes(15));

        AuthResponse response = authService.register(new RegisterRequest("  Ada@Example.COM ", "password1", " Ada "));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900);
        assertThat(response.user().email()).isEqualTo("ada@example.com");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("ada@example.com");
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(userCaptor.getValue().getRole()).isEqualTo(UserRole.USER);
        assertThat(userCaptor.getValue().isEnabled()).isTrue();

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getTokenHash()).isEqualTo(TokenHasher.sha256Hex(response.refreshToken()));
        assertThat(tokenCaptor.getValue().getExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));

        verify(messageRepository).linkUnclaimedMessagesToUser(userCaptor.getValue(), "ada@example.com");
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("ada@example.com", "password1", "Ada")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void loginRejectsUnknownEmailWithSameErrorAsBadPassword() {
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.matches("password1", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ada@example.com", "password1")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void loginIssuesTokensWhenPasswordMatches() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("ada@example.com")
                .passwordHash("hashed")
                .displayName("Ada")
                .build();
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password1", "hashed")).thenReturn(true);
        when(jwtService.createAccessToken(user)).thenReturn("access-token");
        when(jwtService.accessTokenTtl()).thenReturn(Duration.ofMinutes(15));

        AuthResponse response = authService.login(new LoginRequest("ADA@example.com", "password1"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.user().id()).isEqualTo(user.getId());
        assertThat(response.user().role()).isEqualTo(UserRole.USER);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void loginRejectsDisabledUserWithSameErrorAsBadPassword() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("ada@example.com")
                .passwordHash("hashed")
                .displayName("Ada")
                .enabled(false)
                .build();
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password1", "hashed")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ada@example.com", "password1")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

        verify(jwtService, never()).createAccessToken(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refreshRotatesTokenAndRevokesTheOldOne() {
        User user = User.builder().id(UUID.randomUUID()).email("ada@example.com").displayName("Ada").build();
        String raw = "refresh-raw";
        RefreshToken stored = RefreshToken.builder()
                .user(user)
                .tokenHash(TokenHasher.sha256Hex(raw))
                .expiresAt(NOW.plusSeconds(60))
                .build();
        when(refreshTokenRepository.findByTokenHashWithUser(TokenHasher.sha256Hex(raw))).thenReturn(Optional.of(stored));
        when(jwtService.createAccessToken(user)).thenReturn("new-access");
        when(jwtService.accessTokenTtl()).thenReturn(Duration.ofMinutes(15));

        AuthResponse response = authService.refresh(raw);

        assertThat(stored.isRevoked()).isTrue();
        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isNotEqualTo(raw);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void refreshOfRevokedTokenRevokesAllActiveTokens() {
        User user = User.builder().id(UUID.randomUUID()).email("ada@example.com").build();
        String raw = "stolen";
        RefreshToken stored = RefreshToken.builder()
                .user(user)
                .tokenHash(TokenHasher.sha256Hex(raw))
                .expiresAt(NOW.plusSeconds(60))
                .revokedAt(NOW.minusSeconds(5))
                .build();
        when(refreshTokenRepository.findByTokenHashWithUser(TokenHasher.sha256Hex(raw))).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh(raw))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCode.REFRESH_TOKEN_REUSED);

        verify(refreshTokenRepository).revokeAllActiveByUserId(eq(user.getId()), eq(NOW));
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void refreshRejectsDisabledUserWithoutIssuingTokens() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("ada@example.com")
                .displayName("Ada")
                .enabled(false)
                .build();
        String raw = "refresh-raw";
        RefreshToken stored = RefreshToken.builder()
                .user(user)
                .tokenHash(TokenHasher.sha256Hex(raw))
                .expiresAt(NOW.plusSeconds(60))
                .build();
        when(refreshTokenRepository.findByTokenHashWithUser(TokenHasher.sha256Hex(raw))).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh(raw))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);

        assertThat(stored.isRevoked()).isTrue();
        verify(jwtService, never()).createAccessToken(any());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }
}
